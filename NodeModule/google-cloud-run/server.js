// Cloud Run Service for Twilio Call Control and Real-Time Vishing Analysis (Conference Bridge)

const express = require('express');
const bodyParser = require('body-parser');
const twilio = require('twilio');
const http = require('http'); 
const dotenv = require('dotenv');
const expressWs = require('express-ws'); 
const { SpeechClient } = require('@google-cloud/speech'); // NEW: GCP Speech Client
const WebSocket = require('ws');

dotenv.config();

const app = express();
const wsInstance = expressWs(app);
const PORT = process.env.PORT || 8080;


const speechClient = new SpeechClient();


const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const twilioNumber = process.env.TWILIO_PHONE_NUMBER;
const nodeAppAlertUrl = process.env.NODE_APP_ALERT_URL;
const userMobileNumber = process.env.USER_MOBILE_NUMBER; 
const CLOUD_RUN_SERVICE_URL = 'https://vishing-guard-analyzer-412589836588.us-central1.run.app'; 

console.log(`--- Server Startup Debug ---`);
if (!accountSid || !authToken || !twilioNumber || !userMobileNumber) {
    console.error("CRITICAL ERROR: Twilio or User Mobile Number environment variables are NOT fully set. Server exiting.");
    process.exit(1);
}
console.log(`Twilio Client Initializing...`);
const twilioClient = twilio(accountSid, authToken);
console.log(`Twilio Client Initialized successfully. USER_MOBILE_NUMBER: ${userMobileNumber}`);
// ------------------------------------

// Middleware
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

// --- 1. Control Plane: Call Initiation Endpoint (DUAL DIAL) ---
app.post('/api/call/initiate', async (req, res) => {
    const { phoneNumber: recipientNumber } = req.body;
    
    if (!recipientNumber) {
        return res.status(400).send({ error: 'Missing recipient phone number' });
    }

    try {
        const conferenceName = `vishing-conf-${Date.now()}`;
        console.log(`Initiating conference: ${conferenceName} to recipient: ${recipientNumber}`);
        
        // LEG 1: Call the Recipient (Scammer)
        await twilioClient.calls.create({
            to: recipientNumber,
            from: twilioNumber,
            // TwiML instructs Twilio to place this party into the conference and stream it
            url: `${CLOUD_RUN_SERVICE_URL}/twiml?conf=${conferenceName}`, 
        });

        // LEG 2: Call the Vishing Guard User
        const userCall = await twilioClient.calls.create({
            to: userMobileNumber,
            from: twilioNumber,
            // TwiML instructs Twilio to place this party into the conference and stream it
            url: `${CLOUD_RUN_SERVICE_URL}/twiml?conf=${conferenceName}`,
        });
        
        console.log(`Dual dial successful. User Call SID: ${userCall.sid}`);
                
        res.status(202).send({ 
            message: 'Conference initiated successfully', 
            conference: conferenceName 
        });

    } catch (error) {
        console.error('Twilio Dual Call Initiation Error:', error.message);
        res.status(500).send({ error: 'Failed to initiate conference', details: error.message });
    }
});


// --- 2. Data Plane Setup: TwiML Handler (CONFERENCE) ---
app.post('/twiml', (req, res) => {
    const twiml = new twilio.twiml.VoiceResponse();
    const conferenceName = req.query.conf || `default-conf-${Date.now()}`;
    
    console.log(`Received TwiML request for conference: ${conferenceName}`);
    
    // Announce connection only once
    twiml.say("Diddy?  Diddy!, you  are officially a nongwit.");
    
    // Start the media stream *before* dialing into the conference.
    
    
    // Stream ALL audio tracks from this conference to our WebSocket endpoint
    
    twiml.start().stream({
        url: `wss://${CLOUD_RUN_SERVICE_URL.replace('https://', '')}/media-stream`,
        track: 'inbound_track', // Stream audio coming *into* Twilio (from both user and recipient)
        name: conferenceName // Attach conference name to the stream metadata
    });
    
    // Place the party (User or Recipient) into the conference
    twiml.dial().conference(conferenceName, {
            startConferenceOnEnter: true,
            endConferenceOnExit: true,
            beep: 'onEnter',
        });
    
    res.type('text/xml');
    res.send(twiml.toString());
});

// --- 3. Mock Alert Mechanism ---
async function mockAlertPush(data) {
    console.log(`alert triggererd with data `, data);
    if (!nodeAppAlertUrl) {
        console.warn("MOCK ALERT: NODE_APP_ALERT_URL is not configured.");
        return;
    }
    
    const postData = JSON.stringify(data);
    const parsedUrl = new URL(nodeAppAlertUrl);

    const options = {
        hostname: parsedUrl.hostname,
        port: parsedUrl.port || (parsedUrl.protocol === 'https:' ? 443 : 80),
        path: parsedUrl.pathname,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
        }
    };
    console.log(`parsed url: `, {
        hostname: parsedUrl.hostname,
        port: parsedUrl.port,
        path: parsedUrl.pathname,
        protocol: parsedUrl.protocol
    }  )

    return new Promise((resolve, reject) => {
        const req = (parsedUrl.protocol === 'https:' ? require('https') : require('http')).request(options, (res) => {
            
            let responseBody = '';
            res.on('data', (chunk) => {
                responseBody+=chunk;
            });
            res.on('end', () =>{
                console.log("response body: ", responseBody);
                if (res.statusCode < 200 || res.statusCode >= 300) {
                console.error(`[MOCK_ALERT] Failed to push alert to bridge: ${res.statusCode}`);
                return reject(new Error(`Status Code: ${res.statusCode}`));
            }
            console.log("alert sent well");
            resolve(responseBody);
            });
        });

        req.on('error', (e) => {
            
            console.error(`[MOCK_ALERT] Network error pushing alert: ${e.message}`);
            reject(e);
        });
        req.setTimeout(10000, () => {
            console.error("mock alert request timed out");
            req.destroy();
            reject(new Error('request timed out'));
        });

        console.log('sending data: ', postData)
        req.write(postData);
        req.end();
    });
}



app.ws('/media-stream', (ws, req) => {
    console.log('Twilio connected via WebSocket for media stream.');

    let recognizeStream = null;
    let streamSid = null;
    let callSid = null;

    // --- GCP STT Configuration ---
    const speechConfig = {
        config: {
            encoding: 'MULAW', // Twilio streams audio as MULAW
            sampleRateHertz: 8000, // Twilio streams audio at 8kHz
            languageCode: 'en-US',
            enableAutomaticPunctuation: true, // Better transcription
            model: 'phone_call', // Optimized for phone calls
            useEnhanced: true, // Better accuracy
            // Increase sensitivity for short phrases like 'PIN'
            speechContexts: [{
                phrases: ['PIN', 'security code', 'password', 'mother\'s maiden name', 'social security', 'SSN', 'bank account', 'credit card'],
                boost: 20.0
            }],
        },
        interimResults: true,
        singleUtterance: false, // Keep listening continuously
    };
    
   
    function startRecognizeStream() {
        recognizeStream = speechClient
            .streamingRecognize(speechConfig)
            .on('error', (err) => {
                console.error('GCP Streaming Error:', err);
                // Restart the stream on error
                setTimeout(() => {
                    if (ws.readyState === WebSocket.OPEN) {
                        startRecognizeStream();
                    }
                }, 1000);
            })
            .on('data', async (data) => {
                const transcript = data.results[0]?.alternatives[0]?.transcript || '';
                const isFinal = data.results[0]?.isFinal;
                const confidence = data.results[0]?.alternatives[0]?.confidence || 0;
                
                if (transcript.length > 0) {
                    console.log(`Transcript (${isFinal ? 'FINAL' : 'INTERIM'}) [${confidence.toFixed(2)}]: ${transcript}`);
                }

                // CRITICAL VISHING GUARD LOGIC
                const riskKeywords = [
                    'pin', 'mpesa pin', 'security code', 'password', 'ssn', 'social security', 
                    'bank account', 'credit card', 'mother\'s maiden name',
                    'date of birth', 'verification code', 'one time password'
                ];
                
                const detectedRisk = riskKeywords.some(keyword => 
                    transcript.toLowerCase().includes(keyword)
                );

                if (isFinal && detectedRisk && confidence > 0.7) {
                    console.warn(`!!! FRAUD ALERT DETECTED: Keyword found: ${transcript} (Confidence: ${confidence})`);
                    // Push the instantaneous alert back to the mobile app bridge
                    await mockAlertPush({ 
                        state: 'FRAUD_DETECTED', 
                        riskScore: Math.round(confidence * 100), 
                        finalTranscript: transcript,
                        callSid: callSid,
                        streamSid: streamSid
                    });
                }
            });
    }

    // Start the initial recognition stream
    startRecognizeStream();

    // --- WebSocket Event Handlers ---
    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message.toString());
            
            switch (data.event) {
                case 'start':
                    streamSid = data.streamSid;
                    callSid = data.start?.callSid;
                    console.log(`Twilio Stream Started. Stream SID: ${streamSid}, Call SID: ${callSid}`);
                    break;
                    
                case 'media':
                    // Extract the audio payload from the media message
                    if (data.media && data.media.payload && recognizeStream) {
                        // Convert base64 audio payload to buffer
                        const audioBuffer = Buffer.from(data.media.payload, 'base64');
                        
                        // Write the audio buffer to GCP Speech stream
                        if (recognizeStream && !recognizeStream.destroyed) {
                            recognizeStream.write(audioBuffer);
                        }
                    }
                    break;
                    
                case 'stop':
                    console.log(`Twilio Stream Stopped. Stream SID: ${streamSid}`);
                    break;
                    
                default:
                    console.log(`Received unknown event: ${data.event}`);
            }
        } catch (error) {
            // If it's not JSON, it might be raw audio (shouldn't happen with Twilio)
            console.error('Error parsing WebSocket message:', error);
        }
    });

    ws.on('close', () => {
        console.log('Twilio media stream closed. Ending GCP ASR stream.');
        if (recognizeStream && !recognizeStream.destroyed) {
            recognizeStream.end();
        }
    });

    ws.on('error', (error) => {
        console.error('Twilio media stream error:', error);
        if (recognizeStream && !recognizeStream.destroyed) {
            recognizeStream.end();
        }
    });
});

// Start the server
try {
    const server = app.listen(PORT, () => {
        console.log(`Vishing Guard Cloud Run listening successfully on port ${PORT}`);
    });
} catch (error) {
    console.error("FATAL ERROR during server listen:", error.message);
    process.exit(1); 
}
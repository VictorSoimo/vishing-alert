#include <jni.h>
#include <cstdlib>
#include <cstring>
#include <node.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>


// --- Global JNI Cache ---
// These static variables are set in JNI_OnLoad and used by the Node.js thread later.
static JavaVM* g_JavaVM = nullptr;
static jclass g_ServiceClass = nullptr;
const char* SERVICE_CLASS_NAME = "com/vishingalert/app/VishingGuardService";
int pipe_stdout[2];
int pipe_stderr[2];
pthread_t thread_stdout;
pthread_t thread_stderr;
const char *ADBTAG = "NODEJS-MOBILE";

// Function to log from C++ to Android Logcat
#define LOG_TAG "VISHING_GUARD_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// --- JNI Lifecycle Functions ---

// Runs once when System.loadLibrary("native-lib") is called
extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_JavaVM = vm;
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Cache the class reference (Crucial for calling static methods later)
    jclass localClass = env->FindClass(SERVICE_CLASS_NAME);
    if (!localClass) {
        LOGE("Failed to find VishingGuardService class: %s", SERVICE_CLASS_NAME);
        return JNI_ERR;
    }
    g_ServiceClass = (jclass)env->NewGlobalRef(localClass);
    env->DeleteLocalRef(localClass);

    LOGI("JNI_OnLoad successful. Class reference cached.");
    return JNI_VERSION_1_6;
}

// --- JNI Callback Wrapper (Called by Node.js C++ Add-on) ---

// This function is what your Node.js C++ Add-on will call when risk is detected.
// We make it extern "C" so the C++ Add-on can easily link to it.
extern "C" void triggerAndroidAlert(int alertType) {
    if (!g_JavaVM || !g_ServiceClass) {
        LOGE("JNI resources not initialized. Cannot trigger alert.");
        return;
    }

    JNIEnv* env;
    // Check if the current thread (Node.js thread) is attached to the JVM
    jint getEnvStat = g_JavaVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);

    if (getEnvStat == JNI_EDETACHED) {
        // If not attached, attach it.
        if (g_JavaVM->AttachCurrentThread(&env, NULL) != JNI_OK) {
            LOGE("Failed to attach Node.js thread to JVM.");
            return;
        }
    } else if (getEnvStat == JNI_EVERSION) {
        LOGE("Invalid JNI version.");
        return;
    }

    // 1. Get the Method ID: triggerAlert(int type): Unit
    // Signature: (I)V -> (int) Void
    jmethodID methodId = env->GetStaticMethodID(
            g_ServiceClass,
            "triggerAlert",
            "(I)V"
    );

    if (methodId) {
        // 2. Invoke the static method
        env->CallStaticVoidMethod(g_ServiceClass, methodId, (int)alertType);
        LOGI("Alert command sent to Kotlin: Type %d", alertType);
    } else {
        LOGE("Failed to find triggerAlert method!");
    }

    // NOTE: We generally leave the thread attached until Node.js exits gracefully.
}

// --- JNI Function Called by Kotlin (To Start Node.js) ---

// JNI Function Name must match: Java_[Package]_[Class]_MethodName
// Example: Java_com_package_vishingalert_app_VishingGuardService_startNodeWithArguments
extern "C" JNIEXPORT  jint JNICALL
Java_com_vishingalert_app_VishingGuardService_startNodeWithArguments(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arguments) {

    LOGI("JNI function startNoeWithArguments called()");
    jsize argc = env->GetArrayLength(arguments);

    int c_arguments_size = 0;
    for (int i = 0; i < argc ; i++) {
        c_arguments_size += strlen(env->GetStringUTFChars((jstring)env->GetObjectArrayElement(arguments, i), 0));
        c_arguments_size++; // for '\0'
    }
    // 2. Allocate memory for the C-style array of pointers (argv)
    char* args_buffer=(char*)calloc(c_arguments_size, sizeof(char));

    //argv to pass into node.
    char* argv[argc];

    //To iterate through the expected start position of each argument in args_buffer.
    char* current_args_position=args_buffer;

    //Populate the args_buffer and argv.
    for (int i = 0; i < argc ; i++)
    {
        const char* current_argument = env->GetStringUTFChars((jstring)env->GetObjectArrayElement(arguments, i), 0);

        //Copy current argument to its expected position in args_buffer
        strncpy(current_args_position, current_argument, strlen(current_argument));

        //Save current argument start position in argv
        argv[i] = current_args_position;

        //Increment to the next argument's expected position.
        current_args_position += strlen(current_args_position)+1;
    }
    LOGI("calling node start... from JNI");

     jint exit_code = node::Start(argc, argv);
     LOGI("node server is running on port 3000");
    //Start node, with argc and argv.
    return exit_code;




}
extern "C" JNIEXPORT jint JNICALL
Java_com_vishingalert_app_VishingGuardService_stopNode(){

    jint exitCode = 1;

    return exitCode;

}
void *thread_stderr_func(void*) {
    ssize_t redirect_size;
    char buf[2048];
    while((redirect_size = read(pipe_stderr[0], buf, sizeof buf - 1)) > 0) {
        //__android_log will add a new line anyway.
        if(buf[redirect_size - 1] == '\n')
            --redirect_size;
        buf[redirect_size] = 0;
        __android_log_write(ANDROID_LOG_ERROR, ADBTAG, buf);
    }
    return 0;
}

void *thread_stdout_func(void*) {
    ssize_t redirect_size;
    char buf[2048];
    while((redirect_size = read(pipe_stdout[0], buf, sizeof buf - 1)) > 0) {
        //__android_log will add a new line anyway.
        if(buf[redirect_size - 1] == '\n')
            --redirect_size;
        buf[redirect_size] = 0;
        __android_log_write(ANDROID_LOG_INFO, ADBTAG, buf);
    }
    return 0;
}


int start_redirecting_stdout_stderr() {
    //set stdout as unbuffered.
    setvbuf(stdout, 0, _IONBF, 0);
    pipe(pipe_stdout);
    dup2(pipe_stdout[1], STDOUT_FILENO);

    //set stderr as unbuffered.
    setvbuf(stderr, 0, _IONBF, 0);
    pipe(pipe_stderr);
    dup2(pipe_stderr[1], STDERR_FILENO);

    if(pthread_create(&thread_stdout, 0, thread_stdout_func, 0) == -1)
        return -1;
    pthread_detach(thread_stdout);

    if(pthread_create(&thread_stderr, 0, thread_stderr_func, 0) == -1)
        return -1;
    pthread_detach(thread_stderr);

    return 0;
}

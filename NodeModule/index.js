// VishingGuard Node.js Runtime Script (Simplified for Hosting)

// This script now primarily exists to host any advanced Node.js libraries 
// or complex NLP models if needed. For this final implementation, 
// the core networking and alert handling is moved to the Kotlin service 
// to ensure seamless integration with the Android lifecycle.

const TAG = 'VISHING_GUARD_JS';

console.log(TAG + ': Embedded Node.js runtime is active.');
console.log(TAG + ': Network and alert listening logic handled by Kotlin service.');

// This keeps the Node.js runtime process alive until stopNode() is called.
// Do not exit the process here.
setInterval(() => {}, 10000);
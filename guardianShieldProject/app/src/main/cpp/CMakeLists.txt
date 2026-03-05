
cmake_minimum_required(VERSION 3.18.1)
project(vishing-alert)



add_library(native-lib SHARED  native-lib.cpp)

target_include_directories(native-lib  PRIVATE  "${CMAKE_SOURCE_DIR}/../jniLibs/include/node/")


add_library(node-lib SHARED IMPORTED)

set_target_properties(node-lib PROPERTIES
        IMPORTED_LOCATION
        "${CMAKE_SOURCE_DIR}/../jniLibs/bin/${ANDROID_ABI}/libnode.so"
)



# 4. Link your custom library against the Node.js library and NDK logging
find_library(log-lib log)
target_link_libraries(native-lib
        node-lib          # The imported Node.js shared library
        ${log-lib}        # NDK logging utility
)

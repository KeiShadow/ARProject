set(pathToProject D:/GitHub/Bakalarska-prace/NativeCamera)
set(pathToOpenCv  D:/GitHub/Bakalarska-prace/ArucoOpenCV/sdk)

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(../../ArucoOpenCV/sdk/native/jni/include)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp )
#Opencv lib
#add_library(lib_opencv SHARED IMPORTED)

#set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

set(OpenCV_STATIC ON)
set(OpenCV_DIR ../../ArucoOpenCV/sdk/native/jni)
find_package(OpenCV REQUIRED)
target_link_libraries(native-lib ${OpenCV_LIBS})




# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                   native-lib
                   android
                   EGL

                   # Links the target library to the log library
                   # included in the NDK.
                   ${log-lib} )
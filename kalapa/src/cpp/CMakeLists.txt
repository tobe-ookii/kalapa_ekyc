cmake_minimum_required(VERSION 3.4.1)
add_library( # Specifies the name of the library.
        envi
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
       native-lib.cpp)
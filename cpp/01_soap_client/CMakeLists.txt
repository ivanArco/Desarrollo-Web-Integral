cmake_minimum_required(VERSION 3.20)
project(cpp_01_soap_client)

set(CMAKE_CXX_STANDARD 20)

find_package(httplib CONFIG REQUIRED)

add_executable(app main.cpp)
target_link_libraries(app PRIVATE httplib::httplib)

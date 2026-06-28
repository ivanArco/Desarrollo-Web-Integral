cmake_minimum_required(VERSION 3.20)
project(cpp_01_soap_client)

set(CMAKE_CXX_STANDARD 20)

find_package(cpr REQUIRED)
find_package(tinyxml2 REQUIRED)
find_package(httplib CONFIG REQUIRED)

add_executable(app main.cpp)
target_link_libraries(app PRIVATE cpr::cpr tinyxml2::tinyxml2 httplib::httplib)

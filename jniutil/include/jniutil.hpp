#ifndef JVM_JNIUTIL_INCLUDED
#define JVM_JNIUTIL_INCLUDED

#if defined(CPPJVM_LIB)
#define CPPJVM_API 
#elif defined(CPPJVM_EXPORTS)
#define CPPJVM_API __declspec(dllexport)
#else
#define CPPJVM_API __declspec(dllimport)
#endif

#endif

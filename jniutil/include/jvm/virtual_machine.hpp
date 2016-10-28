#ifndef JVM_VIRTUAL_MACHINE_INCLUDED
#define JVM_VIRTUAL_MACHINE_INCLUDED

#include <stdexcept>
#include <jni.h>

#include "jniutil.hpp"

#if defined(CPPJVM_MFC)

#ifndef WINVER				// Allow use of features specific to Windows XP or later.
#define WINVER 0x0501		// Change this to the appropriate value to target other versions of Windows.
#endif

#include <afx.h>
#include <afxstr.h>
#endif

namespace jvm
{
  class virtual_machine;

  CPPJVM_API const virtual_machine &global_vm();
  CPPJVM_API virtual_machine *swap_global_vm(virtual_machine *vm);
  CPPJVM_API bool global_vm_available();
#if defined(CPPJVM_VMCREATE)
  CPPJVM_API void create_global_vm(const std::string &classPath);
#endif /* defined(CPPJVM_VMCREATE) */

	class virtual_machine
	{
		JavaVM *m_jvm;

	public:
		virtual_machine();
#if defined(CPPJVM_VMCREATE)
		virtual_machine(const std::string &classPath);
#endif
#if defined(CPPJVM_VMSET)
		virtual_machine(JavaVM *jvm);
#endif

		~virtual_machine()
			{ destroy(); }

#if defined(CPPJVM_VMCREATE)
		void create(const std::string &classPath);
#endif
		void destroy();

		JNIEnv *env(JNIEnv *e = 0) const;
    void check_exception(JNIEnv *e = 0) const;

#if defined(CPPJVM_STL)
		jstring string(const std::string &v, JNIEnv *e = 0) const;
		jstring string(const std::wstring &v, JNIEnv *e = 0) const;
		std::wstring wstring(jstring v, JNIEnv *e = 0) const;
		std::string string(jstring v, JNIEnv *e = 0) const;

		void throw_exception(const std::string &msg, JNIEnv *e = 0) const;
		void throw_exception(const std::wstring &msg, JNIEnv *e = 0) const;
#endif

#if defined(CPPJVM_MFC)
		jstring string(const CStringA &v, JNIEnv *e = 0) const;
		jstring string(const CStringW &v, JNIEnv *e = 0) const;
		CStringW wstring(jstring v, JNIEnv *e = 0) const;
		CStringA string(jstring v, JNIEnv *e = 0) const;

		void throw_exception(const CStringA &msg, JNIEnv *e = 0) const;
		void throw_exception(const CStringW &msg, JNIEnv *e = 0) const;
#endif

	};

  struct global_init_enlist_base
  {
  public:
    virtual void startup() = 0;
    virtual void cleanup() = 0;
  };

  template <class T>
	class global_init_enlist : public global_init_enlist_base
	{
	  T *impl_;

	public:
    global_init_enlist()
      : impl_(0) { enlist(this); }

    ~global_init_enlist()
      { cleanup(); }

    void startup()
    {
      if (impl_ == 0)
          impl_ = new T();
    }

    void cleanup()
    {
      if (impl_ != 0)
      {
        T *d = impl_;
        impl_ = 0;
        delist(this);
        try { delete d; } catch (...) { }
      }
    }
  };
	
	void enlist(global_init_enlist_base *init);
	void delist(global_init_enlist_base *init);
}

#endif

#ifndef JVM_VIRTUAL_MACHINE_GLOBAL_IMPL_INCLUDED
#define JVM_VIRTUAL_MACHINE_GLOBAL_IMPL_INCLUDED

#include "jvm/virtual_machine.hpp"

static jvm::virtual_machine *g_vm = 0;
const jvm::virtual_machine &jvm::global_vm()
{
  if (g_vm == 0)
		throw std::logic_error("No virtual machine available");

  return *g_vm;
}

jvm::virtual_machine *jvm::swap_global_vm(jvm::virtual_machine *vm)
{
  jvm::virtual_machine *o = g_vm;
  g_vm = vm;
	return o;
}

bool jvm::global_vm_available()
{
  return (g_vm != 0);
}

#if defined(CPPJVM_VMCREATE)
static jvm::virtual_machine g_vm_default;
void jvm::create_global_vm(const std::string &classPath)
{
	if (g_vm != 0)
		throw std::logic_error("Global virtual machine already initialized");

	g_vm_default.create(classPath);
	g_vm = &g_vm_default;
	
	global_init_startup();
}
#endif /* defined(CPPJVM_VMCREATE) */

#endif

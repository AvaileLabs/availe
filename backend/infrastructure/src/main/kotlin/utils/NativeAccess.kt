package com.availelabs.utils

import java.io.File
import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.charset.StandardCharsets

internal object NativeAccess {

    private val linker: Linker = Linker.nativeLinker()
    private val symbolLookup: SymbolLookup = linker.defaultLookup()

    fun getLinuxMaxMapCount(): Long {
        val procFile = File("/proc/sys/vm/max_map_count")
        if (!procFile.exists()) {
            throw IllegalStateException("File /proc/sys/vm/max_map_count does not exist. Is this a Linux system?")
        }
        val content = procFile.readText(StandardCharsets.UTF_8).trim()
        return content.toLong()
    }

    fun getMacOsSysctlInt(parameterName: String): Int {
        val sysctlByNameAddress = symbolLookup.find("sysctlbyname")
            .orElseThrow { IllegalStateException("Function 'sysctlbyname' not found. Is this macOS?") }

        val functionDescriptor = FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG
        )

        val methodHandle: MethodHandle = linker.downcallHandle(sysctlByNameAddress, functionDescriptor)

        Arena.ofConfined().use { arena ->
            val nameSegment = arena.allocateFrom(parameterName)
            val oldLengthSegment = arena.allocate(ValueLayout.JAVA_LONG)
            val intSize = ValueLayout.JAVA_INT.byteSize()
            oldLengthSegment.set(ValueLayout.JAVA_LONG, 0, intSize)

            val initialResult = methodHandle.invokeExact(
                nameSegment,
                MemorySegment.NULL,
                oldLengthSegment,
                MemorySegment.NULL,
                0L
            ) as Int

            if (initialResult != 0) {
                throw IllegalStateException("Failed to query size for sysctl '$parameterName'. Return code: $initialResult")
            }

            val dataSize = oldLengthSegment.get(ValueLayout.JAVA_LONG, 0)
            val oldDataSegment = arena.allocate(dataSize)

            val finalResult = methodHandle.invokeExact(
                nameSegment,
                oldDataSegment,
                oldLengthSegment,
                MemorySegment.NULL,
                0L
            ) as Int

            if (finalResult != 0) {
                throw IllegalStateException("Failed to retrieve value for sysctl '$parameterName'. Return code: $finalResult")
            }

            return oldDataSegment.get(ValueLayout.JAVA_INT, 0)
        }
    }
}
package io.github.jwharm.cairobindings;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * Utility class that loads the native cairo library, and contains functions to
 * create method handles and allocate memory.
 */
public class Interop {

    private final static SymbolLookup symbolLookup;
    private final static Linker linker = Linker.nativeLinker();

	// Load the cairo library during class initialization.
	// This is triggered by calling Interop.ensureInitialized(), and is
	// guaranteed to run only once.
    static {
        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        symbolLookup = name -> loaderLookup.find(name).or(() -> linker.defaultLookup().find(name));
        
        LibLoad.loadLibrary("cairo");
    }
    
    /**
     * Ensures the Interop class initializer has run, and the cairo library is loaded.
     */
    public static void ensureInitialized() {
    }
	
    /**
     * Creates a method handle that is used to call the native function with 
     * the provided name and function descriptor.
     * @param name Name of the native function
     * @param fdesc Function descriptor of the native function
     * @param variadic Whether the function has varargs
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc, boolean variadic) {
        // Copied from jextract-generated code
        var handle = symbolLookup
                .find(name)
                .map(addr -> variadic ? VarargsInvoker.make(addr, fdesc) : linker.downcallHandle(addr, fdesc))
                .orElse(null);
        return handle;
    }
    
    /**
     * Creates a method handle that is used to call the native function at
     * the provided memory address.
     * @param symbol Memory address of the native function
     * @param fdesc Function descriptor of the native function
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(MemorySegment symbol, FunctionDescriptor fdesc) {
        return linker.downcallHandle(symbol, fdesc);
    }

    /**
     * Allocate a native string using SegmentAllocator.allocateUtf8String(String).
     * @param string the string to allocate as a native string (utf8 char*)
     * @param allocator the segment allocator to use
     * @return the allocated MemorySegment
     */
    public static MemorySegment allocateNativeString(String string, SegmentAllocator allocator) {
        return string == null ? MemorySegment.NULL : allocator.allocateUtf8String(string);
    }
    
    /**
     * Returns a Java string from native memory using {@code MemorySegment.getUtf8String()}.
     * If an error occurs or when the native address is NULL, null is returned.
     * @param address The memory address of the native String (\0-terminated char*).
     * @return A String or null
     */
    public static String getStringFrom(MemorySegment address) {
        try {
            if (!MemorySegment.NULL.equals(address)) {
                return address.getUtf8String(0);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Read an array of Strings with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @return Array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address, int length) {
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = address.getUtf8String(i * ValueLayout.ADDRESS.byteSize());
        }
        return result;
    }

    /**
     * Read an array of pointers with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @return Array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address, int length) {
        MemorySegment[] result = new MemorySegment[length];
        for (int i = 0; i < length; i++) {
            result[i] = address.getAtIndex(ValueLayout.ADDRESS, i);
        }
        return result;
    }

    /**
     * Read an array of booleans with the given length from native memory
     * The array is read from native memory as an array of integers with value 1 or 0,
     * and converted to booleans with 1 = true and 0 = false.
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of booleans
     */
    public static boolean[] getBooleanArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        int[] intArray = getIntegerArrayFrom(address, length, scope);
        boolean[] array = new boolean[intArray.length];
        for (int c = 0; c < intArray.length; c++)
            array[c] = (intArray[c] != 0);
        return array;
    }

    /**
     * Read an array of bytes with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of bytes
     */
    public static byte[] getByteArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        byte[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_BYTE);
        return array;
    }

    /**
     * Read an array of chars with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of chars
     */
    public static char[] getCharacterArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        char[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_CHAR);
        return array;
    }

    /**
     * Read an array of doubles with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of doubles
     */
    public static double[] getDoubleArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        double[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_DOUBLE);
        return array;
    }

    /**
     * Read an array of floats with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of floats
     */
    public static float[] getFloatArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        float[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_FLOAT);
        return array;
    }

    /**
     * Read an array of integers with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        int[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_INT);
        return array;
    }

    /**
     * Read an array of longs with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of longs
     */
    public static long[] getLongArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        long[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_LONG);
        return array;
    }

    /**
     * Read an array of shorts with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @return array of shorts
     */
    public static short[] getShortArrayFrom(MemorySegment address, long length, SegmentScope scope) {
        short[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_SHORT);
        return array;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of strings (NUL-terminated utf8 char*).
     * @param strings Array of Strings
     * @param zeroTerminated Whether to add a NUL at the end the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(String[] strings, boolean zeroTerminated, SegmentAllocator allocator) {
        int length = zeroTerminated ? strings.length + 1 : strings.length;
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < strings.length; i++) {
            var cString = strings[i] == null ? MemorySegment.NULL : allocator.allocateUtf8String(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, cString);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, strings.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    /**
     * Converts the boolean[] array into an int[] array, and calls {@link #allocateNativeArray(int[], boolean, SegmentAllocator)}.
     * Each boolean value "true" is converted 1, boolean value "false" to 0.
     * @param array Array of booleans
     * @param zeroTerminated When true, an (int) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(boolean[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }
        return allocateNativeArray(intArray, zeroTerminated, allocator);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of bytes.
     * @param array The array of bytes
     * @param zeroTerminated When true, a (byte) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(byte[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        byte[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_BYTE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of chars.
     * @param array The array of chars
     * @param zeroTerminated When true, a (char) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(char[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        char[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_CHAR, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of doubles.
     * @param array The array of doubles
     * @param zeroTerminated When true, a (double) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(double[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        double[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_DOUBLE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated When true, a (float) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(float[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        float[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_FLOAT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated When true, a (int) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(int[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        int[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_INT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of longs.
     * @param array The array of longs
     * @param zeroTerminated When true, a (long) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(long[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        long[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_LONG, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of shorts.
     * @param array The array of shorts
     * @param zeroTerminated When true, a (short) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(short[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        short[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_SHORT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of pointers (from Proxy instances).
     * @param array The array of Proxy instances
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        MemorySegment[] addressArray = new MemorySegment[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i] == null ? MemorySegment.NULL : array[i].handle();
        }
        return allocateNativeArray(addressArray, zeroTerminated, allocator);
    }
    
    /**
     * Allocates and initializes an (optionally NULL-terminated) array
     * of structs (from Proxy instances). The actual memory segments (not 
     * the pointers) are copied into the array.
     * @param array The array of Proxy instances
     * @param layout The memory layout of the object type
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param allocator the allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array, MemoryLayout layout, boolean zeroTerminated, SegmentAllocator allocator) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        MemorySegment memorySegment = allocator.allocateArray(layout, length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                MemorySegment element = MemorySegment.ofAddress(array[i].handle().address(), layout.byteSize(), memorySegment.scope());
                memorySegment.asSlice(i * layout.byteSize()).copyFrom(element);
            } else {
                memorySegment.asSlice(i * layout.byteSize(), layout.byteSize()).fill((byte) 0);
            }
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of memory addresses.
     * @param array The array of addresses
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(MemorySegment[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i] == null ? MemorySegment.NULL : array[i]);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemorySegment.NULL);
        }
        return memorySegment;
    }
    
    // Adapted from code that was generated by jextract
    private static class VarargsInvoker {
        private static final MethodHandle INVOKE_MH;
        private final MemorySegment symbol;
        private final FunctionDescriptor function;
        private static final SegmentAllocator THROWING_ALLOCATOR = (x, y) -> { throw new AssertionError("should not reach here"); };

        private VarargsInvoker(MemorySegment symbol, FunctionDescriptor function) {
            this.symbol = symbol;
            this.function = function;
        }

        static {
            try {
                INVOKE_MH = MethodHandles.lookup().findVirtual(VarargsInvoker.class, "invoke", MethodType.methodType(Object.class, SegmentAllocator.class, Object[].class));
            } catch (ReflectiveOperationException e) {
                throw new InteropException(e);
            }
        }

        static MethodHandle make(MemorySegment symbol, FunctionDescriptor function) {
            VarargsInvoker invoker = new VarargsInvoker(symbol, function);
            MethodHandle handle = INVOKE_MH.bindTo(invoker).asCollector(Object[].class, function.argumentLayouts().size() + 1);
            MethodType mtype = MethodType.methodType(function.returnLayout().isPresent() ? carrier(function.returnLayout().get(), true) : void.class);
            for (MemoryLayout layout : function.argumentLayouts()) {
                mtype = mtype.appendParameterTypes(carrier(layout, false));
            }
            mtype = mtype.appendParameterTypes(Object[].class);
            boolean needsAllocator = function.returnLayout().isPresent() &&
                    function.returnLayout().get() instanceof GroupLayout;
            if (needsAllocator) {
                mtype = mtype.insertParameterTypes(0, SegmentAllocator.class);
            } else {
                handle = MethodHandles.insertArguments(handle, 0, THROWING_ALLOCATOR);
            }
            return handle.asType(mtype);
        }

        static Class<?> carrier(MemoryLayout layout, boolean ret) {
            if (layout instanceof ValueLayout valLayout) {
                return (ret || valLayout.carrier() != MemorySegment.class) ?
                        valLayout.carrier() : MemorySegment.class;
            } else if (layout instanceof GroupLayout) {
                return MemorySegment.class;
            } else {
                throw new AssertionError("Cannot get here!");
            }
        }

        // This method is used from a MethodHandle (INVOKE_MH).
        @SuppressWarnings("unused")
        private Object invoke(SegmentAllocator allocator, Object[] args) throws Throwable {
            // one trailing Object[]
            int nNamedArgs = function.argumentLayouts().size();
            assert(args.length == nNamedArgs + 1);
            // The last argument is the array of vararg collector
            Object[] unnamedArgs = (Object[]) args[args.length - 1];

            int argsCount = nNamedArgs + unnamedArgs.length;
            MemoryLayout[] argLayouts = new MemoryLayout[nNamedArgs + unnamedArgs.length];

            int pos = 0;
            for (pos = 0; pos < nNamedArgs; pos++) {
                argLayouts[pos] = function.argumentLayouts().get(pos);
            }
            
            // Unwrap the java-gi types to their memory address or primitive value
            Object[] unwrappedArgs = new Object[unnamedArgs.length];
            for (int i = 0; i < unnamedArgs.length; i++) {
                unwrappedArgs[i] = unwrapJavaTypes(unnamedArgs[i]);
            }

            assert pos == nNamedArgs;
            for (Object o: unwrappedArgs) {
                argLayouts[pos] = variadicLayout(normalize(o.getClass()));
                pos++;
            }
            assert pos == argsCount;

            FunctionDescriptor f = (function.returnLayout().isEmpty()) ?
                    FunctionDescriptor.ofVoid(argLayouts) :
                    FunctionDescriptor.of(function.returnLayout().get(), argLayouts);
            MethodHandle mh = linker.downcallHandle(symbol, f);
            boolean needsAllocator = function.returnLayout().isPresent() &&
                    function.returnLayout().get() instanceof GroupLayout;
            if (needsAllocator) {
                mh = mh.bindTo(allocator);
            }
            // flatten argument list so that it can be passed to an asSpreader MH
            Object[] allArgs = new Object[nNamedArgs + unwrappedArgs.length];
            System.arraycopy(args, 0, allArgs, 0, nNamedArgs);
            System.arraycopy(unwrappedArgs, 0, allArgs, nNamedArgs, unwrappedArgs.length);

            return mh.asSpreader(Object[].class, argsCount).invoke(allArgs);
        }

        private static Class<?> unboxIfNeeded(Class<?> clazz) {
            if (clazz == Boolean.class) {
                return boolean.class;
            } else if (clazz == Void.class) {
                return void.class;
            } else if (clazz == Byte.class) {
                return byte.class;
            } else if (clazz == Character.class) {
                return char.class;
            } else if (clazz == Short.class) {
                return short.class;
            } else if (clazz == Integer.class) {
                return int.class;
            } else if (clazz == Long.class) {
                return long.class;
            } else if (clazz == Float.class) {
                return float.class;
            } else if (clazz == Double.class) {
                return double.class;
            } else {
                return clazz;
            }
        }

        private Class<?> promote(Class<?> c) {
            if (c == byte.class || c == char.class || c == short.class || c == int.class) {
                return long.class;
            } else if (c == float.class) {
                return double.class;
            } else {
                return c;
            }
        }

        private Class<?> normalize(Class<?> c) {
            c = unboxIfNeeded(c);
            if (c.isPrimitive()) {
                return promote(c);
            }
            if (MemorySegment.class.isAssignableFrom(c)) {
                return MemorySegment.class;
            }
            throw new IllegalArgumentException("Invalid type for ABI: " + c.getTypeName());
        }

        private MemoryLayout variadicLayout(Class<?> c) {
            if (c == long.class) {
                return ValueLayout.JAVA_LONG;
            } else if (c == double.class) {
                return ValueLayout.JAVA_DOUBLE;
            } else if (MemorySegment.class.isAssignableFrom(c)) {
                return ValueLayout.ADDRESS;
            } else {
                throw new IllegalArgumentException("Unhandled variadic argument class: " + c);
            }
        }
        
        // Unwrap the java types to their memory address or primitive value.
        // Arrays are allocated to native memory as-is (no additional NUL is appended: the caller must do this)
        private Object unwrapJavaTypes(Object o) {
            if (o == null) {
                return MemorySegment.NULL;
            }
            if (o instanceof MemorySegment[] addresses) {
                return Interop.allocateNativeArray(addresses, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof Boolean bool) {
                return bool ? 1 : 0;
            }
            if (o instanceof boolean[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof byte[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof char[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof double[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof float[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof int[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof long[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof short[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof Proxy proxy) {
                return proxy.handle();
            }
            if (o instanceof Proxy[] proxys) {
                return Interop.allocateNativeArray(proxys, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof java.lang.String string) {
                return Interop.allocateNativeString(string, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof java.lang.String[] strings) {
                return Interop.allocateNativeArray(strings, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            return o;
        }
    }
}

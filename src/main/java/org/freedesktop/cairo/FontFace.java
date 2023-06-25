package org.freedesktop.cairo;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * The base class for font faces.
 * <p>
 * FontFace represents a particular font at a particular weight, slant, and
 * other characteristic but no size, transformation, or size.
 * <p>
 * Font faces are created using font-backend-specific constructors, typically of
 * the form {@code FontFaceClass.create()}, or implicitly using the toy text API
 * by way of {@link Context#selectFontFace(String, FontSlant, FontWeight)}. The
 * resulting face can be accessed using {@link Context#getFontFace()}.
 * <p>
 * A FontFace specifies all aspects of a font other than the size or font matrix
 * (a font matrix is used to distort a font by shearing it or scaling it
 * unequally in the two directions) . A font face can be set on a Context by
 * using {@link Context#setFontFace(FontFace)} the size and font matrix are set
 * with {@link Context#setFontSize(double)} and
 * {@link Context#setFontMatrix(Matrix)}.
 * <p>
 * There are various types of font faces, depending on the font backend they
 * use. The type of a font face can be queried using {@link FontFace#getType()}.
 * 
 * @see ScaledFont
 * @since 1.0
 */
public class FontFace extends Proxy {

    static {
        Interop.ensureInitialized();
    }

    // Keeps user data keys and values
    private final UserDataStore userDataStore;

    /**
     * Constructor used internally to instantiate a java FontFace object for a
     * native {@code cairo_font_face_t} instance
     * 
     * @param address the memory address of the native {@code cairo_font_face_t}
     *                instance
     */
    public FontFace(MemorySegment address) {
        super(address);
        setDestroyFunc("cairo_font_face_destroy");
        userDataStore = new UserDataStore(address.scope());
    }

    /**
     * Checks whether an error has previously occurred for this font face
     * 
     * @return {@link Status#SUCCESS} or another error such as
     *         {@link Status#NO_MEMORY}.
     * @since 1.0
     */
    public Status status() {
        try {
            int result = (int) cairo_font_face_status.invoke(handle());
            return Status.of(result);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle cairo_font_face_status = Interop.downcallHandle("cairo_font_face_status",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    /**
     * Returns the type of the backend used to create a font face. See
     * {@link FontType} for available types.
     * 
     * @return The type of the FontFace.
     * @since 1.2
     */
    public FontType getType() {
        try {
            int result = (int) cairo_font_face_get_type.invoke(handle());
            return FontType.of(result);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle cairo_font_face_get_type = Interop.downcallHandle("cairo_font_face_get_type",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    /**
     * Increases the reference count on the FontFace by one. This prevents the
     * FontFace from being destroyed until a matching call to
     * {@code cairo_font_face_destroy()} is made.
     * 
     * @since 1.0
     */
    void reference() {
        try {
            cairo_font_face_reference.invoke(handle());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle cairo_font_face_reference = Interop.downcallHandle("cairo_font_face_reference",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    /**
     * Attach user data to the font face. This method will generate and return a
     * {@link UserDataKey}. To update the user data for the same key, call
     * {@link #setUserData(UserDataKey, Object)}. To remove user data from a font
     * face, call this function with {@code null} for {@code userData}.
     * 
     * @param userData the user data to attach to the font face. {@code userData}
     *                 can be any Java object, but if it is a primitive type, a
     *                 {@link MemorySegment} or a {@link Proxy} instance, it will be
     *                 stored as cairo user data in native memory as well.
     * @return the key that the user data is attached to
     * @since 1.4
     */
    public UserDataKey setUserData(Object userData) {
        UserDataKey key = UserDataKey.create(this);
        return setUserData(key, userData);
    }

    /**
     * Attach user data to the font face. To remove user data from a font face, call
     * this function with the key that was used to set it and {@code null} for
     * {@code userData}.
     * 
     * @param key      the key to attach the user data to
     * @param userData the user data to attach to the font face. {@code userData}
     *                 can be any Java object, but if it is a primitive type, a
     *                 {@link MemorySegment} or a {@link Proxy} instance, it will be
     *                 stored as cairo user data in native memory as well.
     * @return the key
     * @throws NullPointerException if {@code key} is {@code null}
     * @since 1.4
     */
    public UserDataKey setUserData(UserDataKey key, Object userData) {
        Status status;
        userDataStore.set(key, userData);
        try {
            int result = (int) cairo_font_face_set_user_data.invoke(handle(), key.handle(),
                    userDataStore.dataSegment(userData), MemorySegment.NULL);
            status = Status.of(result);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        if (status == Status.NO_MEMORY) {
            throw new RuntimeException(status.toString());
        }
        return key;
    }

    private static final MethodHandle cairo_font_face_set_user_data = Interop
            .downcallHandle("cairo_font_face_set_user_data", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    /**
     * Return user data previously attached to the font face using the specified
     * key. If no user data has been attached with the given key this function
     * returns {@code null}.
     * 
     * @param key the UserDataKey the user data was attached to
     * @return the user data previously attached or {@code null}
     * @since 1.4
     */
    public Object getUserData(UserDataKey key) {
        return key == null ? null : userDataStore.get(key);
    }
}

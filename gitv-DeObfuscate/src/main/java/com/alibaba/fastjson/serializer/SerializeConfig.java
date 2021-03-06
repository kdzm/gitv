package com.alibaba.fastjson.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONStreamAware;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.deserializer.Jdk8DateCodec;
import com.alibaba.fastjson.parser.deserializer.OptionalCodec;
import com.alibaba.fastjson.util.ASMUtils;
import com.alibaba.fastjson.util.IdentityHashMap;
import com.alibaba.fastjson.util.ServiceLoader;
import com.alibaba.fastjson.util.TypeUtils;
import java.io.File;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Clob;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class SerializeConfig {
    private static boolean awtError = false;
    public static final SerializeConfig globalInstance = new SerializeConfig();
    private static boolean jdk8Error = false;
    private static boolean oracleJdbcError = false;
    private boolean asm;
    private ASMSerializerFactory asmFactory;
    private final IdentityHashMap<Type, ObjectSerializer> serializers;
    private String typeKey;

    public String getTypeKey() {
        return this.typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public final ObjectSerializer createASMSerializer(Class<?> clazz) throws Exception {
        return this.asmFactory.createJavaBeanSerializer(clazz, null);
    }

    public ObjectSerializer createJavaBeanSerializer(Class<?> clazz) {
        if (!Modifier.isPublic(clazz.getModifiers())) {
            return new JavaBeanSerializer(clazz);
        }
        boolean asm = this.asm;
        if ((asm && this.asmFactory.classLoader.isExternalClass(clazz)) || clazz == Serializable.class || clazz == Object.class) {
            asm = false;
        }
        JSONType annotation = (JSONType) clazz.getAnnotation(JSONType.class);
        if (!(annotation == null || annotation.asm())) {
            asm = false;
        }
        if (asm && !ASMUtils.checkName(clazz.getName())) {
            asm = false;
        }
        if (asm) {
            for (Field field : clazz.getDeclaredFields()) {
                JSONField annotation2 = (JSONField) field.getAnnotation(JSONField.class);
                if (annotation2 != null && !ASMUtils.checkName(annotation2.name())) {
                    asm = false;
                    break;
                }
            }
        }
        if (asm) {
            try {
                ObjectSerializer asmSerializer = createASMSerializer(clazz);
                if (asmSerializer != null) {
                    return asmSerializer;
                }
            } catch (ClassCastException e) {
            } catch (Throwable e2) {
                JSONException jSONException = new JSONException("create asm serializer error, class " + clazz, e2);
            }
        }
        return new JavaBeanSerializer(clazz);
    }

    public boolean isAsmEnable() {
        return this.asm;
    }

    public void setAsmEnable(boolean asmEnable) {
        if (!ASMUtils.IS_ANDROID) {
            this.asm = asmEnable;
        }
    }

    public static SerializeConfig getGlobalInstance() {
        return globalInstance;
    }

    public SerializeConfig() {
        this(1024);
    }

    public SerializeConfig(int tableSize) {
        this.asm = !ASMUtils.IS_ANDROID;
        this.typeKey = JSON.DEFAULT_TYPE_KEY;
        this.serializers = new IdentityHashMap(1024);
        try {
            if (this.asm) {
                this.asmFactory = new ASMSerializerFactory();
            }
        } catch (NoClassDefFoundError e) {
            this.asm = false;
        } catch (ExceptionInInitializerError e2) {
            this.asm = false;
        }
        put(Boolean.class, BooleanCodec.instance);
        put(Character.class, CharacterCodec.instance);
        put(Byte.class, IntegerCodec.instance);
        put(Short.class, IntegerCodec.instance);
        put(Integer.class, IntegerCodec.instance);
        put(Long.class, LongCodec.instance);
        put(Float.class, FloatCodec.instance);
        put(Double.class, DoubleSerializer.instance);
        put(BigDecimal.class, BigDecimalCodec.instance);
        put(BigInteger.class, BigIntegerCodec.instance);
        put(String.class, StringCodec.instance);
        put(byte[].class, PrimitiveArraySerializer.instance);
        put(short[].class, PrimitiveArraySerializer.instance);
        put(int[].class, PrimitiveArraySerializer.instance);
        put(long[].class, PrimitiveArraySerializer.instance);
        put(float[].class, PrimitiveArraySerializer.instance);
        put(double[].class, PrimitiveArraySerializer.instance);
        put(boolean[].class, PrimitiveArraySerializer.instance);
        put(char[].class, PrimitiveArraySerializer.instance);
        put(Object[].class, ObjectArrayCodec.instance);
        put(Class.class, MiscCodec.instance);
        put(SimpleDateFormat.class, MiscCodec.instance);
        put(Currency.class, new MiscCodec());
        put(TimeZone.class, MiscCodec.instance);
        put(InetAddress.class, MiscCodec.instance);
        put(Inet4Address.class, MiscCodec.instance);
        put(Inet6Address.class, MiscCodec.instance);
        put(InetSocketAddress.class, MiscCodec.instance);
        put(File.class, MiscCodec.instance);
        put(Appendable.class, AppendableSerializer.instance);
        put(StringBuffer.class, AppendableSerializer.instance);
        put(StringBuilder.class, AppendableSerializer.instance);
        put(Charset.class, ToStringSerializer.instance);
        put(Pattern.class, ToStringSerializer.instance);
        put(Locale.class, ToStringSerializer.instance);
        put(URI.class, ToStringSerializer.instance);
        put(URL.class, ToStringSerializer.instance);
        put(UUID.class, ToStringSerializer.instance);
        put(AtomicBoolean.class, AtomicCodec.instance);
        put(AtomicInteger.class, AtomicCodec.instance);
        put(AtomicLong.class, AtomicCodec.instance);
        put(AtomicReference.class, ReferenceCodec.instance);
        put(AtomicIntegerArray.class, AtomicCodec.instance);
        put(AtomicLongArray.class, AtomicCodec.instance);
        put(WeakReference.class, ReferenceCodec.instance);
        put(SoftReference.class, ReferenceCodec.instance);
    }

    public void addFilter(Class<?> clazz, SerializeFilter filter) {
        ObjectSerializer serializer = getObjectWriter(clazz);
        if (serializer instanceof SerializeFilterable) {
            ((SerializeFilterable) serializer).addFilter(filter);
        }
    }

    public ObjectSerializer getObjectWriter(Class<?> clazz) {
        AutowiredObjectSerializer autowired;
        ObjectSerializer objectSerializer = (ObjectSerializer) this.serializers.get(clazz);
        if (objectSerializer == null) {
            try {
                for (Object o : ServiceLoader.load(AutowiredObjectSerializer.class, Thread.currentThread().getContextClassLoader())) {
                    if (o instanceof AutowiredObjectSerializer) {
                        autowired = (AutowiredObjectSerializer) o;
                        for (Type forType : autowired.getAutowiredFor()) {
                            put(forType, autowired);
                        }
                    }
                }
            } catch (ClassCastException e) {
            }
            objectSerializer = (ObjectSerializer) this.serializers.get(clazz);
        }
        if (objectSerializer == null) {
            ClassLoader classLoader = JSON.class.getClassLoader();
            if (classLoader != Thread.currentThread().getContextClassLoader()) {
                try {
                    for (Object o2 : ServiceLoader.load(AutowiredObjectSerializer.class, classLoader)) {
                        if (o2 instanceof AutowiredObjectSerializer) {
                            autowired = (AutowiredObjectSerializer) o2;
                            for (Type forType2 : autowired.getAutowiredFor()) {
                                put(forType2, autowired);
                            }
                        }
                    }
                } catch (ClassCastException e2) {
                }
                objectSerializer = (ObjectSerializer) this.serializers.get(clazz);
            }
        }
        if (objectSerializer == null) {
            if (Map.class.isAssignableFrom(clazz)) {
                put(clazz, MapSerializer.instance);
            } else if (List.class.isAssignableFrom(clazz)) {
                put(clazz, ListSerializer.instance);
            } else if (Collection.class.isAssignableFrom(clazz)) {
                put(clazz, CollectionCodec.instance);
            } else if (Date.class.isAssignableFrom(clazz)) {
                put(clazz, DateCodec.instance);
            } else if (JSONAware.class.isAssignableFrom(clazz)) {
                put(clazz, JSONAwareSerializer.instance);
            } else if (JSONSerializable.class.isAssignableFrom(clazz)) {
                put(clazz, JSONSerializableSerializer.instance);
            } else if (JSONStreamAware.class.isAssignableFrom(clazz)) {
                put(clazz, MiscCodec.instance);
            } else if (clazz.isEnum() || (clazz.getSuperclass() != null && clazz.getSuperclass().isEnum())) {
                put(clazz, EnumSerializer.instance);
            } else if (clazz.isArray()) {
                Class<?> componentType = clazz.getComponentType();
                put(clazz, new ArraySerializer(componentType, getObjectWriter(componentType)));
            } else if (Throwable.class.isAssignableFrom(clazz)) {
                put(clazz, new JavaBeanSerializer(clazz, null, TypeUtils.getSerializeFeatures(clazz) | SerializerFeature.WriteClassName.mask));
            } else if (TimeZone.class.isAssignableFrom(clazz)) {
                put(clazz, MiscCodec.instance);
            } else if (Appendable.class.isAssignableFrom(clazz)) {
                put(clazz, AppendableSerializer.instance);
            } else if (Charset.class.isAssignableFrom(clazz)) {
                put(clazz, ToStringSerializer.instance);
            } else if (Enumeration.class.isAssignableFrom(clazz)) {
                put(clazz, EnumerationSerializer.instance);
            } else if (Calendar.class.isAssignableFrom(clazz)) {
                put(clazz, CalendarCodec.instance);
            } else if (Clob.class.isAssignableFrom(clazz)) {
                put(clazz, ClobSeriliazer.instance);
            } else if (TypeUtils.isPath(clazz)) {
                put(clazz, ToStringSerializer.instance);
            } else if (Iterable.class.isAssignableFrom(clazz) || Iterator.class.isAssignableFrom(clazz)) {
                put(clazz, MiscCodec.instance);
            } else {
                String className = clazz.getName();
                if (className.startsWith("java.awt.") && AwtCodec.support(clazz)) {
                    if (!awtError) {
                        try {
                            put(Class.forName("java.awt.Color"), AwtCodec.instance);
                            put(Class.forName("java.awt.Font"), AwtCodec.instance);
                            put(Class.forName("java.awt.Point"), AwtCodec.instance);
                            put(Class.forName("java.awt.Rectangle"), AwtCodec.instance);
                        } catch (Throwable th) {
                            awtError = true;
                        }
                    }
                    return AwtCodec.instance;
                }
                if (!jdk8Error && (className.startsWith("java.time.") || className.startsWith("java.util.Optional"))) {
                    try {
                        put(Class.forName("java.time.LocalDateTime"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.LocalDate"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.LocalTime"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.ZonedDateTime"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.OffsetDateTime"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.OffsetTime"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.ZoneOffset"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.ZoneRegion"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.Period"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.Duration"), Jdk8DateCodec.instance);
                        put(Class.forName("java.time.Instant"), Jdk8DateCodec.instance);
                        put(Class.forName("java.util.Optional"), OptionalCodec.instance);
                        put(Class.forName("java.util.OptionalDouble"), OptionalCodec.instance);
                        put(Class.forName("java.util.OptionalInt"), OptionalCodec.instance);
                        put(Class.forName("java.util.OptionalLong"), OptionalCodec.instance);
                        objectSerializer = (ObjectSerializer) this.serializers.get(clazz);
                        if (objectSerializer != null) {
                            return objectSerializer;
                        }
                    } catch (Throwable th2) {
                        jdk8Error = true;
                    }
                }
                if (!oracleJdbcError && className.startsWith("oracle.sql.")) {
                    try {
                        put(Class.forName("oracle.sql.DATE"), DateCodec.instance);
                        put(Class.forName("oracle.sql.TIMESTAMP"), DateCodec.instance);
                        objectSerializer = (ObjectSerializer) this.serializers.get(clazz);
                        if (objectSerializer != null) {
                            return objectSerializer;
                        }
                    } catch (Throwable th3) {
                        oracleJdbcError = true;
                    }
                }
                boolean isCglibProxy = false;
                boolean isJavassistProxy = false;
                Class[] interfaces = clazz.getInterfaces();
                int length = interfaces.length;
                int i = 0;
                while (i < length) {
                    String interfaceName = interfaces[i].getName();
                    if (interfaceName.equals("net.sf.cglib.proxy.Factory") || interfaceName.equals("org.springframework.cglib.proxy.Factory")) {
                        isCglibProxy = true;
                        break;
                    } else if (interfaceName.equals("javassist.util.proxy.ProxyObject") || interfaceName.equals("org.apache.ibatis.javassist.util.proxy.ProxyObject")) {
                        isJavassistProxy = true;
                        break;
                    } else {
                        i++;
                    }
                }
                if (isCglibProxy || isJavassistProxy) {
                    ObjectSerializer superWriter = getObjectWriter(clazz.getSuperclass());
                    put(clazz, superWriter);
                    return superWriter;
                }
                put(clazz, createJavaBeanSerializer(clazz));
            }
            objectSerializer = (ObjectSerializer) this.serializers.get(clazz);
        }
        return objectSerializer;
    }

    public final ObjectSerializer get(Type key) {
        return (ObjectSerializer) this.serializers.get(key);
    }

    public boolean put(Type key, ObjectSerializer value) {
        return this.serializers.put(key, value);
    }
}

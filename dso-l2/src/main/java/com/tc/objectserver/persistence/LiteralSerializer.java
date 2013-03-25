package com.tc.objectserver.persistence;

import org.terracotta.corestorage.Serializer;

import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.EnumInstance;
import com.tc.object.dna.impl.UTF8ByteCompressedDataHolder;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.objectserver.managedobject.CDSMValue;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tim
 */
public class LiteralSerializer extends Serializer<Object> {
  public static final LiteralSerializer INSTANCE = new LiteralSerializer();

  private static final int LONG_SIZE = Long.SIZE / Byte.SIZE;
  private static final int INT_SIZE = Integer.SIZE / Byte.SIZE;
  private static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;

  private static enum Type {
    LONG {
      @Override
      public Long deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.getLong();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Long) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + LONG_SIZE);
          buffer.put((byte) ordinal()).putLong((Long)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Long.class;
      }
    }, INT {
      @Override
      public Integer deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.getInt();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Integer) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + INT_SIZE);
          buffer.put((byte) ordinal()).putInt((Integer)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Integer.class;
      }
    }, SHORT {
      @Override
      public Short deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.getShort();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Short) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + SHORT_SIZE);
          buffer.put((byte) ordinal()).putShort((Short)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Short.class;
      }
    }, BYTE {
      @Override
      public Byte deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.get();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof Byte) {
          ByteBuffer buffer = ByteBuffer.allocate(2);
          buffer.put((byte) ordinal()).put((Byte)object).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return Byte.class;
      }
    }, STRING {
      @Override
      public String deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return buffer.asCharBuffer().toString();
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof String) {
          String s = (String) object;
          ByteBuffer buffer = ByteBuffer.allocate(1 + s.length() * 2);
          buffer.put((byte)ordinal());
          buffer.position(buffer.position() + buffer.asCharBuffer().put(s).position() * 2);
          buffer.flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return String.class;
      }
    }, OBJECTID {
      @Override
      public ObjectID deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        return new ObjectID(buffer.getLong());
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof ObjectID) {
          ByteBuffer buffer = ByteBuffer.allocate(1 + LONG_SIZE);
          buffer.put((byte)ordinal()).putLong(((ObjectID)object).toLong()).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return ObjectID.class;
      }
    }, UTF8BYTES {
      @Override
      Object deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return new UTF8ByteDataHolder(bytes);
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof UTF8ByteDataHolder) {
          byte[] bytes = ((UTF8ByteDataHolder)object).getBytes();
          ByteBuffer buffer = ByteBuffer.allocate(1 + INT_SIZE + bytes.length);
          buffer.put((byte)ordinal()).putInt(bytes.length).put(bytes).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return UTF8ByteDataHolder.class;
      }
    }, UT8COMPRESSEDBYTES {
      @Override
      Object deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        return new UTF8ByteCompressedDataHolder(bytes, buffer.getInt(), buffer.getInt(), buffer.getInt());
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof UTF8ByteCompressedDataHolder) {
          UTF8ByteCompressedDataHolder holder = (UTF8ByteCompressedDataHolder)object;
          byte[] bytes = holder.getBytes();
          ByteBuffer buffer = ByteBuffer.allocate(1 + INT_SIZE + bytes.length + INT_SIZE * 3);
          buffer.put((byte)ordinal()).putInt(bytes.length).put(bytes);
          buffer.putInt(holder.getUncompressedStringLength()).putInt(holder.getStringLength()).putInt(holder.getStringHash()).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return UTF8ByteCompressedDataHolder.class;
      }
    }, ENUM {
      @Override
      Object deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        byte[] valueBytes = new byte[buffer.getInt()];
        buffer.get(valueBytes);
        byte[] classInstanceBytes = new byte[buffer.getInt()];
        buffer.get(classInstanceBytes);
        return new EnumInstance(new ClassInstance(new UTF8ByteDataHolder(classInstanceBytes)),
            new UTF8ByteDataHolder(valueBytes));
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof EnumInstance) {
          EnumInstance enumInstance = (EnumInstance) object;
          byte[] valueBytes = enumInstance.getEnumName().getBytes();
          byte[] classInstanceBytes = enumInstance.getClassInstance().getName().getBytes();
          ByteBuffer buffer = ByteBuffer.allocate(1 + INT_SIZE * 2 + classInstanceBytes.length + valueBytes.length);
          buffer.put((byte)ordinal()).putInt(valueBytes.length).put(valueBytes).putInt(classInstanceBytes.length).put(classInstanceBytes).flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return EnumInstance.class;
      }
    }, CDSMValue {
      @Override
      Object deserialize(final ByteBuffer buffer) {
        if (buffer.get() != ordinal()) {
          throw new AssertionError();
        }
        ObjectID objectID = new ObjectID(buffer.getLong());
        long creationTime = buffer.getLong();
        long lastAccessedTime = buffer.getLong();
        if (buffer.get() == 1) {
          return new CDSMValue(objectID, creationTime, lastAccessedTime, buffer.getLong(), buffer.getLong());
        } else {
          return new CDSMValue(objectID, creationTime, lastAccessedTime, 0, 0);
        }
      }

      @Override
      ByteBuffer serialize(final Object object) {
        if (object instanceof CDSMValue) {
          CDSMValue cdsmValue = (CDSMValue) object;
          ByteBuffer buffer = ByteBuffer.allocate(2 + LONG_SIZE * 5);
          buffer.put((byte) ordinal());
          buffer.putLong(cdsmValue.getObjectID().toLong()).putLong(cdsmValue.getCreationTime()).putLong(cdsmValue.getLastAccessedTime());
          if (cdsmValue.getTimeToIdle() != 0 || cdsmValue.getTimeToLive() != 0) {
            buffer.put((byte)1);
            buffer.putLong(cdsmValue.getTimeToIdle()).putLong(cdsmValue.getTimeToLive());
          } else {
            buffer.put((byte)0);
          }
          buffer.flip();
          return buffer;
        } else {
          throw new AssertionError();
        }
      }

      @Override
      Class<?> toClass() {
        return CDSMValue.class;
      }
    };

    abstract Object deserialize(ByteBuffer buffer);

    abstract ByteBuffer serialize(Object object);

    abstract Class<?> toClass();
  }

  private static final Map<Class<?>, Type> classToType;
  static {
    classToType = new HashMap<Class<?>, Type>();
    for (Type type : Type.values()) {
      classToType.put(type.toClass(), type);
    }
  }


  @Override
  public Object recover(final ByteBuffer buffer) {
    return Type.values()[buffer.duplicate().get()].deserialize(buffer);
  }

  @Override
  public ByteBuffer transform(final Object o) {
    if (o == null) {
      throw new IllegalArgumentException("Serializing a null is not supported.");
    }
    if (!classToType.containsKey(o.getClass())) {
      throw new IllegalArgumentException("Unknown type " + o + " class " + o.getClass());
    }
    return classToType.get(o.getClass()).serialize(o);
  }

  @Override
  public boolean equals(final Object left, final ByteBuffer right) {
    return left.equals(recover(right));
  }
}
package run.rook.chi.data;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;

import org.agrona.DirectBuffer;

public final class DataTypeUtil {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static byte[] toBytes(DirectBuffer value, int valueLength) {
		byte[] bytes = new byte[valueLength];
		value.getBytes(0, bytes);
		return bytes;
	}
	
	public static byte[] toBytes(BigInteger value, DataType dataType) {
		switch (dataType) {
		case I8:
		case I16:
		case I32:
		case I64:
		case U8:
		case U16:
		case U32:
		case U64:
			return Arrays.copyOf(value.toByteArray(), dataType.getSize());
		default:
			throw new IllegalArgumentException("Invalid DataType: " + dataType);
		}
	}
	
	public static long toLong(DirectBuffer value, int valueLength, DataType dataType) {
		return toBigInteger(value, valueLength, dataType).longValue();
	}
	
	public static int toInteger(DirectBuffer value, int valueLength, DataType dataType) {
		return toBigInteger(value, valueLength, dataType).intValue();
	}
	
	public static short toShort(DirectBuffer value, int valueLength, DataType dataType) {
		return toBigInteger(value, valueLength, dataType).shortValue();
	}
	
	public static short toByte(DirectBuffer value, int valueLength, DataType dataType) {
		return toBigInteger(value, valueLength, dataType).byteValue();
	}
	
	public static BigInteger toBigInteger(DirectBuffer value, int valueLength, DataType dataType) {
		switch (dataType) {
		case I8:
		case I16:
		case I32:
		case I64:
			return toSignedInteger(value, valueLength);
		default:
			return toUnsignedInteger(value, valueLength);
		}
	}

	private static BigInteger toSignedInteger(DirectBuffer value, int valueLength) {
		byte[] bytes = toBytes(value, valueLength);
		reverse(bytes);
		boolean negative = (bytes[0] & 0b10000000) > 0;
		if (negative) {
			return new BigInteger(bytes).subtract(BigInteger.ONE).not();
		} else {
			return new BigInteger(bytes);
		}
	}

	private static BigInteger toUnsignedInteger(DirectBuffer value, int valueLength) {
		byte[] bytes = toBytes(value, valueLength);
		reverse(bytes);
		return new BigInteger(bytes);
	}

	public static String toString(DirectBuffer value, int valueLength, DataType dataType) {
		switch(dataType) {
		case UTF8:
			return new String(toBytes(value, valueLength), UTF8);
		case I8:
		case I16:
		case I32:
		case I64:
		case U8:
		case U16:
		case U32:
		case U64:
			return toBigInteger(value, valueLength, dataType).toString();
		default:
			return Base64.getEncoder().encodeToString(toBytes(value, valueLength));
		}
	}
	
	public static byte[] toBytes(String value, DataType dataType) {
		switch(dataType) {
		case UTF8:
			return value.getBytes(UTF8);
		case I8:
		case I16:
		case I32:
		case I64:
		case U8:
		case U16:
		case U32:
		case U64:
			return toBytes(new BigInteger(value), dataType);
		case BLOB:
			return Base64.getDecoder().decode(value);
		default:
			throw new IllegalArgumentException("Invalid DataType: " + dataType);
		}
	}

	private static void reverse(byte[] arr) {
		for (int i = 0; i < arr.length / 2; i++) {
			byte tmp = arr[i];
			arr[i] = arr[arr.length - i - 1];
			arr[arr.length - i - 1] = tmp;
		}
	}

	private DataTypeUtil() {

	}
}

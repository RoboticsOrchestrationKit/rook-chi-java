package run.rook.chi.ws;

import run.rook.chi.data.DataType;

public class NamedValue {
	private String name;
	private DataType dataType;
	private String value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "NamedValue [name=" + name + ", dataType=" + dataType + ", value=" + value + "]";
	}

}

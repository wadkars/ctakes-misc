package org.apache.ctakes.pipelines;

public class CTakesFilePart {
	private final String fileName;
	private final int part;
	private final String input;
	public CTakesFilePart(String fileName, int part, String input) {
		super();
		this.fileName = fileName;
		this.part = part;
		this.input = input;
	}
	public String getFileName() {
		return fileName;
	}
	public int getPart() {
		return part;
	}
	public String getInput() {
		return input;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CTakesFilePart other = (CTakesFilePart) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return this.fileName +"-" + this.part;
	}
	
}

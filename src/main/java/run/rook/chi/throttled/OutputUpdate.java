package run.rook.chi.throttled;

class OutputUpdate {
	public OutputType type;
	public String name;
	public final OutputLongUpdate longUpdate = new OutputLongUpdate();
	public final OutputBufferUpdate bufferUpdate;

	public OutputUpdate(int maxValueSize) {
		bufferUpdate = new OutputBufferUpdate(maxValueSize);
	}
}
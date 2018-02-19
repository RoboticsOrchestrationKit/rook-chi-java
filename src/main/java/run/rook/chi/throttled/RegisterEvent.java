package run.rook.chi.throttled;

import run.rook.chi.ValueListener;

class RegisterEvent {
	public final RegisterEventType type;
	public final ValueListener listener;

	public RegisterEvent(RegisterEventType type, ValueListener listener) {
		this.type = type;
		this.listener = listener;
	}
}
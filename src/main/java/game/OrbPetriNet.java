package game;

import org.jspace.ActualField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

public class OrbPetriNet implements Runnable {
	//Implemented using naive approach.
	private Server server;
	private Space buffSpace;
	private boolean team;
	
	public OrbPetriNet(Server server, Space buffSpace, boolean team) {
		this.server = server;
		this.buffSpace = buffSpace;
		this.team = team;
	}
	
	public void run() {
		Space[] spaces = new Space[7];
		for (Space space : spaces) {
			space = new SequentialSpace();
		}
		new Thread(new Split(new Space[] {spaces[0]}, new Space[] {spaces[1], spaces[2]})).start();
		new Thread(new TopOrb(new Space[] {spaces[1]}, new Space[] {spaces[3]})).start();
		new Thread(new BottomOrb(new Space[] {spaces[2]}, new Space[] {spaces[4]})).start();
		new Thread(new ConsumeOrbs(new Space[] {spaces[3], spaces[4]}, new Space[] {spaces[5]})).start();
		new Thread(new Heal(new Space[] {spaces[5]}, new Space[] {spaces[6]})).start();
		new Thread(new SpawnOrbs(new Space[] {spaces[6]}, new Space[] {spaces[0]})).start();
	}
	
	private class Activity implements Runnable {
		private Space[] inputs;
		private Space[] outputs;
		public Activity(Space[] inputs, Space[] outputs) {
			this.inputs = inputs;
			this.outputs = outputs;
		}
		public void run() {
			for (Space input : inputs) {
				try {
					input.get(new ActualField("token"));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			performTask();
			for (Space output : outputs) {
				try {
					output.put("token");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		public void performTask() {}
	}
	
	private class Split extends Activity {
		public Split(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
	}
	
	private class TopOrb extends Activity {
		public TopOrb(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			
		}
	}
	
	private class BottomOrb extends Activity {
		public BottomOrb(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			
		}
	}
	
	private class ConsumeOrbs extends Activity {
		public ConsumeOrbs(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			
		}
	}
	
	private class Heal extends Activity {
		public Heal(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			try {
				buffSpace.put(team, "heal");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class SpawnOrbs extends Activity {
		public SpawnOrbs(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			
		}
	}

}

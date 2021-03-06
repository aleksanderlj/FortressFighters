package game;

import org.jspace.ActualField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.util.List;
import java.util.Random;

public class OrbPetriNet implements Runnable {
	//Implemented using naive approach.
	private Server server;
	private Space buffSpace;
	private Space beforeBuffSpace;
	private boolean team;
	
	public OrbPetriNet(Server server, Space buffSpace, boolean team) {
		this.server = server;
		this.buffSpace = buffSpace;
		this.team = team;
	}
	
	public void run() {
		Space[] spaces = new Space[7];
		for (int i = 0; i < 7; i++) {
			spaces[i] = new SequentialSpace();
		}
		beforeBuffSpace = spaces[5];
		new Thread(new Split(new Space[] {spaces[0]}, new Space[] {spaces[1], spaces[2]})).start();
		new Thread(new TopOrb(new Space[] {spaces[1]}, new Space[] {spaces[3]})).start();
		new Thread(new BottomOrb(new Space[] {spaces[2]}, new Space[] {spaces[4]})).start();
		new Thread(new ConsumeOrbs(new Space[] {spaces[3], spaces[4]}, new Space[] {spaces[5]})).start();
		new Thread(new ConflictSolver(new Space[] {spaces[5]},
				new Heal(new Space[] {spaces[5]}, new Space[] {spaces[6]}),
				new GhostBuff(new Space[] {spaces[5]}, new Space[] {spaces[6]}),
				new BulletBuff(new Space[] {spaces[5]}, new Space[] {spaces[6]}),
				new ShieldBuff(new Space[] {spaces[5]}, new Space[] {spaces[6]})
		)).start();
		new Thread(new SpawnOrbs(new Space[] {spaces[6]}, new Space[] {spaces[0]})).start();
		try {
			spaces[0].put("token");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void reset() {
		for (int i = 0; i < 3; i++) {
			try {
				beforeBuffSpace.put("token");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private abstract class Activity implements Runnable {
		protected Space[] inputs;
		protected Space[] outputs;
		private boolean loop = true;

		public Activity(Space[] inputs, Space[] outputs) {
			this.inputs = inputs;
			this.outputs = outputs;
		}

		public Activity(Space[] inputs, Space[] outputs, boolean loop) {
			this.inputs = inputs;
			this.outputs = outputs;
			this.loop = loop;
		}

		public void run() {
			while (!server.isGameOver()) {
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
				if(!loop){
					break;
				}
			}
		}
		public void performTask() {}
	}

	// Created to counter jSpace not being truly random when competing for tokens
	private class ConflictSolver implements Runnable {
		protected Activity[] activities;
		protected Space[] inputs;

		public ConflictSolver(Space[] inputs, Activity... activities){
			this.inputs = inputs;
			this.activities = activities;
		}

		@Override
		public void run() {
			while (!server.isGameOver()) {
				for (Space input : inputs) {
					try {
						input.query(new ActualField("token"));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				activities[new Random().nextInt(activities.length)].run();
			}
		}
	}
	
	private class Split extends Activity {
		public Split(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
	}
	
	private class TopOrb extends Activity {
		public TopOrb(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			try {
				buffSpace.get(new ActualField(team), new ActualField(true));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class BottomOrb extends Activity {
		public BottomOrb(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			try {
				buffSpace.get(new ActualField(team), new ActualField(false));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ConsumeOrbs extends Activity {
		public ConsumeOrbs(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			server.getOrbController().resetOrbHolder(team, true);
			server.getOrbController().resetOrbHolder(team, false);
		}
	}
	
	private class Heal extends Activity {
		public Heal(Space[] inputs, Space[] outputs) {super(inputs, outputs, false);}
		public void performTask() {
			try {
				buffSpace.put(team, "heal");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class GhostBuff extends Activity {
		public GhostBuff(Space[] inputs, Space[] outputs) {super(inputs, outputs, false);}
		public void performTask() {
			try {
				buffSpace.put(team, "ghost");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class BulletBuff extends Activity {
		public BulletBuff(Space[] inputs, Space[] outputs) {super(inputs, outputs, false);}
		public void performTask() {
			try {
				buffSpace.put(team, "bullets");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class ShieldBuff extends Activity {
		public ShieldBuff(Space[] inputs, Space[] outputs) {super(inputs, outputs, false);}
		public void performTask() {
			try {
				buffSpace.put(team, "shield");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class SpawnOrbs extends Activity {
		public SpawnOrbs(Space[] inputs, Space[] outputs) {super(inputs, outputs);}
		public void performTask() {
			server.getOrbController().createNewOrb();
			server.getOrbController().createNewOrb();
		}
	}

}

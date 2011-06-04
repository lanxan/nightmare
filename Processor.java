import java.lang.Thread;
import java.awt.Point;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import static java.lang.Math.*;

public class Processor implements Runnable{
	private Stage stage;
	private Clock clock;
	//private State myState;
	private long time;
	private EventConnect eventConnect;
	private int HEIGHT;
	private int WIDTH;

	private BlockingQueue<Printable[]> queue;
	private ArrayList<Enemy> enemyList;
	private ArrayList<Bullet> bulletList;
	private ArrayList<Bullet> playerBulletList;

	private Hero player;

	private void runInstructions(){
		Instruction[] insts = stage.get();
		if(insts == null){
			return ;
		}
		for(int i=0;i<insts.length;i++){
			String instType = insts[i].getInstType();
			if(instType.equals("enemy")){
				EnemyInstruction inst = insts[i];
				int enemyId = inst.getEnemyId();
				Enemy e = EnemyFactory.getEnemy(inst);
				/* do not use i */
				for(int j=enemyList.size();j<enemyId;j++)
					enemyList.add(null);
				Enemy enemy =  EnemyFactory.getEnemy(inst);
				enemyList.add(enemyId, enemy );
			}
			else if(instType.equals("bullet")){
				BulletInstruction inst = insts[i];
				int enemyId = inst.getEnemyId();
				if(enemyId < enemyList.size() && enemyList.get(enemyId) != null){
					String bulletType = inst.getBulletType();
					int bulletId= inst.getBulletId();
					Enemy enemy = enemyList.get(enemyId);
					/* do not use i */
					for(int j=bulletList.size();j<bulletId;j++)
						bulletList.add(null);
					Bullet bullet = BulletFactory.getBullet(inst,enemy);
					bulletList.add(bulletId,bullet);
				}
			}
			else if(instType.equals("route")){
				RouteInstruction inst = insts[i];
				String targetType = inst.getTargetType();
				int    targetId   = inst.getTargetId();
				if(targetType.equals("enemy")){
					if(targetId < enemyList.size() && enemyList.get(targetId) != null){
						Enemy target = enemyList.get(targetId);
						Route route = RouteFactory.getRoute(inst,target,player.getPosition());
						target.setRoute(route);
					}
				}
				else if(targetType.equals("bullet")){
					if(targetId < bulletList.size() && bulletList.get(targetId) != null){
						Bullet target = bulletList.get(targetId);
						Route route = RouteFactory.getRoute(inst,target,player.getPosition());
						target.setRoute(route);
					}
				}
			}
			/*
			else if(instType.equals("end")){
				System.out.println("The End");
				clock.stop();
			}
			*/
		}
	}
	private void calcBullet(){
		Point position;
		for(Bullet bullet : bulletList){
			if(bullet ==null)
				continue;
			
			bullet.move();
			position = bullet.getPosition();
		}
		for(Bullet bullet : playerBulletList){
			if(bullet ==null)
				continue;
			
			bullet.move();
			position = bullet.getPosition();
		}
	}
	private void calcEnemy(){
		Point position;
		for(Enemy enemy : enemyList){
			if(enemy == null)
				continue;
			enemy.move();
			position = enemy.getPosition();
		}
	}
	private void calcPlayer(){
		player.move();
		Bullet playerBullet = player.shoot();
		if(playerBullet != null){
			playerBulletList.add( playerBullet);
		}
	}
	private void collision(){
		for(Bullet bullet : bulletList){
			if(bullet == null)
				continue;
			if(bullet.collision(player)){
				int life = player.crash();
				EventConnect.dispatch("crash");
				if(life < 0){
					//eventConnect.dispatch("end");
				}
			}
		}
		for(Bullet bullet : playerBulletList){
			if(bullet == null)
				continue;
			
			Object[] enemies = enemyList.toArray();
			for(int i=0;i<enemies.length;i++){
				Enemy enemy = (Enemy)enemies[i];
				if(enemy == null)
					continue;
				if(bullet.collision(enemy)){
					if(enemy.attacted( player.getPower())){
						enemy.crash();
						enemyList.remove(enemy);
					}
				}				
			}
		}
	}
	private void outputToScreem(){
		ArrayList<Printable> output = new ArrayList<Printable>();
		output.addAll(enemyList);
		output.addAll(bulletList);
		output.addAll(playerBulletList);
		output.add(player);
		ArrayList<Printable> temp = new ArrayList<Printable>();
		for(Printable toPrint: output){
			if(toPrint != null)
				temp.add(toPrint);
		}
		Object[] obj = temp.toArray();
		Printable[] array = new Printable[obj.length];
		for(int i=0;i<obj.length;i++){
			array[i] = (Printable)obj[i];
		}
		queue.offer(array);
	}
	/*recycle trash over the screen*/
	private void recycle(){
		int x;
		int y;
		
		Object[] bullets = bulletList.toArray();
		for(int i=0;i<bullets.length;i++){
			Bullet bullet = (Bullet)bullets[i];
			if(bullet == null)
				continue;
			x = (int)bullet.getPosition().getX();
			y = (int)bullet.getPosition().getY();
			if( calcRecycle(x, y) )
				bulletList.remove(bullet);
		}
		
		Object[] playerBullets = playerBulletList.toArray();
		for(int i=0;i<playerBullets.length;i++){
			Bullet bullet = (Bullet)playerBullets[i];
			if(bullet == null)
				continue;
			x = (int)bullet.getPosition().getX();
			y = (int)bullet.getPosition().getY();
			if( calcRecycle(x, y) )
				playerBulletList.remove(bullet);
		}
		
		Object[] enemies = enemyList.toArray();
		for(int i=0;i<enemies.length;i++){
			Enemy enemy = (Enemy)enemies[i];
			if(enemy == null)
				continue;
			x = (int)enemy.getPosition().getX();
			y = (int)enemy.getPosition().getY();
			if( calcRecycle(x, y) )
				enemyList.remove(enemy);
		}
	}
	private boolean calcRecycle(int x, int y){
		boolean xOver;
		boolean yOver;
		xOver = ( (x<0 || x>WIDTH) );
		yOver = ( (y<0 || y>HEIGHT) );
		if( xOver || yOver)
			return true;
		else
			return false;
	}
	
	public void stateCheck(){
		if(player.isPause()){
			EventConnect.dispatch("pause");
			clock.pause();
		}
	}
	public void run(){
		while(true){
			try{
				time = clock.next(time);
			}
			catch(DelayException e){
			}
			stateCheck();
			runInstructions();
			calcBullet();
			calcEnemy();
			calcPlayer();
			collision();
			recycle();
			outputToScreem();
		}
	}
	public Processor(Stage stage,
					Clock clock,
					Hero player,
					BlockingQueue<Printable[]> queue
					){
		enemyList  = new ArrayList<Enemy>();
		bulletList = new ArrayList<Bullet>();
		playerBulletList = new ArrayList<Bullet>();
		this.queue = queue;
		this.player= player; 
		this.stage = stage;
		this.clock = clock;
		this.time = 0;
		this.HEIGHT = 600;
		this.WIDTH  = 450;
		
	}
}


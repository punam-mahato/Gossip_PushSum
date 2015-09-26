import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.util.Timeout
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.math
import Worker._
import scala.concurrent.{Future, blocking}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import Project2._


case class createTopology()
case class startGossip(gossipMessage:String)
case class startPushSum()
case class STOP()

object Master {
	
	def props(acsys: ActorSystem, numNodes: Int, topology: String, algorithm:String):Props =
    Props(classOf[Master], acsys, numNodes, topology, algorithm)
}



class Master(acsys: ActorSystem, numNodes: Int, topology: String, algorithm:String) extends Actor{

	
	var gossipActorsList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]

	def receive ={
		case createTopology()=> setUpNodes(numNodes,topology)
		case startGossip(gossipMessage)=> 
			val r = (scala.util.Random).nextInt(gossipActorsList.length)
			println("\n"+"Start gossip from " + r +"th node in the gossipActorsList!" +"\n")
			gossipActorsList(r) ! beginGossip(gossipMessage)
		case startPushSum()=>
			val r = (scala.util.Random).nextInt(gossipActorsList.length)
			gossipActorsList(r) ! beginPushSum()

		case STOP() =>
			println ("messageCount exceeded the limit.")
			context.system.shutdown()
	}


	def setUpNodes(numNodes:Int, topology:String){
		if ((topology == "3DGrid")||(topology=="Imperfect3DGrid")){
			val x = scala.math.ceil(scala.math.cbrt(numNodes)).toInt
			val newnumNodes:Int=x*x*x
		}

		for (i<-0 until numNodes){
			val acref =  context.actorOf(Props(classOf[Worker],numNodes, topology) )
			gossipActorsList+=acref

		}

		topology match{
			case "FullNetwork"=>{
				for (i<-0 until numNodes){
					var neighboursList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
					for (j<-0 until numNodes){
						if(j!=i){
							neighboursList+= gossipActorsList(j)
						}
					}
					
					gossipActorsList(i) ! setYourNeighboursList(neighboursList)
				}
			}

			case "Line" => {
				for (i <- 0 until numNodes) {
		          var neighboursList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
		          if(i == 0) {
		            neighboursList += gossipActorsList(i + 1)
		          }
		          else if(i == (numNodes - 1)) {
		            neighboursList += gossipActorsList(i - 1)
		          }
		          else {
		            neighboursList += gossipActorsList(i - 1)
		            neighboursList += gossipActorsList(i + 1)
		          }

		          gossipActorsList(i) ! setYourNeighboursList(neighboursList)
				}
			}
			//case Grid3D
			//case Imperfect3DGrid
		}

	


	}



}
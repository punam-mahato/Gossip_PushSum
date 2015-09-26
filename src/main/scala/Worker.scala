import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.actor._ 
import Master._
import scala.collection.mutable.ListBuffer
import scala.collection.TraversableOnce
import scala.util.control._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import Worker._
import Project2._

object Worker {
	case class setYourNeighboursList(neighboursList:ArrayBuffer[ActorRef])
	case class beginGossip(gossipMessage:String)
	case class passMessage(gossipMessage:String)
	case class beginPushSum()
	
}

class Worker(numNodes:Int, topology:String) extends Actor{
	var messageCount:Int = 0
	var myNeighboursList:ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
	def receive = {
		case setYourNeighboursList(neighboursList) =>
			myNeighboursList= neighboursList

		case beginGossip(gossipMessage) =>
			val r = (scala.util.Random).nextInt(myNeighboursList.length)
			println("gossip started & passing message to node: " + r +"\n")
			myNeighboursList(r) ! passMessage(gossipMessage)

		case passMessage(gossipMessage) =>
			if (messageCount==10){

				context.actorSelection("..") ! STOP()
			}
			else{
				println("got message from node: " + sender)
				val r = (scala.util.Random).nextInt(myNeighboursList.length)
				println("passing message to node: " + r)
			myNeighboursList(r) ! passMessage(gossipMessage)
			messageCount +=1
			}

		case beginPushSum =>


	}
}
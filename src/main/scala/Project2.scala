import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import Master._
import akka.util.Timeout
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.math
import com.typesafe.config.ConfigFactory
import scala.concurrent.{Future, blocking}
import akka.pattern.ask


object Project2 {
	
	def main(args: Array[String]){
		

		if (args.length==3){
			val mastersystem = ActorSystem("MasterActorSystem", ConfigFactory.load().getConfig("masterSystem"))
			val nodes = args(0).toInt
			val master_actor = mastersystem.actorOf(Master.props(mastersystem, nodes, args(1), args(2)), name = "MasterActor")
			
			master_actor ! CreateTopology()

			val gossipMessage:String="Hello"
			if (args(2)=="gossip"){				
				master_actor ! StartGossip(gossipMessage)
			}
			if (args(2)=="pushsum"){				
				master_actor ! StartPushSum()				
			}

		}

		else {
			println("Provide 3 arguments: numNodes, topology:<>, algorithm: <gossip|pushsum>")
		}
	}
}
import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.actor._ 

import scala.collection.mutable.ListBuffer
import scala.collection.TraversableOnce
import scala.util.control._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory



case class SetYourNeighboursList(neighboursList:ArrayBuffer[ActorRef])
case class BeginGossip(gossipMessage:String)
case class PassMessage(gossipMessage:String)
case class BeginPushSum(startSum:Double, startWeight:Double)
case class PassSum(receivedSum:Double, receivedWeight:Double)




class Worker(index:Int) extends Actor{
	//val index:Int = index
	var gossipMessageCount:Int = 0
	var sum:Double= index
	var weight:Double = 0
	var ratioPrev3: Double =0
	var ratioPrev2: Double =0
	var ratioPrev1: Double =0
	var currentRatio: Double =0
	var myNeighboursList:ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
	

	def receive = {
		case SetYourNeighboursList(neighboursList) =>				
			myNeighboursList= neighboursList

		case BeginGossip(gossipMessage) =>			
			val r = (scala.util.Random).nextInt(myNeighboursList.length)
			println("\n Gossip started & passing message to neighbour: " + r +"\n")
			myNeighboursList(r) ! PassMessage(gossipMessage)


		case PassMessage(gossipMessage) =>
			if (gossipMessageCount==10){

				context.actorSelection("..") ! STOPGossip()
			}
			else{
				//println("got message from node: " + sender)
				gossipMessageCount +=1
				val r = (scala.util.Random).nextInt(myNeighboursList.length)
				//println("passing message to neighbour: " + r)
				println("MygossipMessageCount: " + gossipMessageCount)
			myNeighboursList(r) ! PassMessage(gossipMessage)
			
			}

		case BeginPushSum(startSum, startWeight) => 		
			//println("Sum at starting node: " + 0 )
			sum = sum + startSum
			weight = weight + startWeight
			println("starting withsum: " + sum)
			println("starting with weight: " + weight)			
			sum = sum/2
			weight= weight/2
			currentRatio= sum/weight
			val r = (scala.util.Random).nextInt(myNeighboursList.length)
			println ("Pushsum started & passing sum to neighbour: " + r + "\n")
			myNeighboursList(r) ! PassSum(sum, weight)

		case PassSum(receivedSum, receivedWeight) =>
			//println("\ngot message from node: " + sender)
			sum = sum + receivedSum
			weight = weight + receivedWeight

			//println("sum: " + sum)
			//println("weight: " + weight)

			ratioPrev3= ratioPrev2
			ratioPrev2= ratioPrev1
			ratioPrev1= currentRatio



			currentRatio= sum/weight
			//println("My index: "+ index +" current sum: " + sum + "  weight: " +weight + "  ratio: " + currentRatio)
			sum = sum/2
			weight = weight/2

			

			if ((currentRatio - ratioPrev3).abs <= 0.0000000001){
				println("\ncurrentRatio: " + currentRatio)
				println("ratioPrev1: " + ratioPrev1)
				println("ratioPrev2: " + ratioPrev2)
				println("ratioPrev3: " + ratioPrev3)
				println("\nThe difference of the ratio in three consecutive rounds: " + (currentRatio -ratioPrev3).abs)
				
				context.actorSelection("..") ! STOPPushSum()

			}

			else{
				val r = (scala.util.Random).nextInt(myNeighboursList.length)
				//println("passing message to neighbour: " + r)
				myNeighboursList(r) ! PassSum(sum, weight)
			}
	
	
	}
}
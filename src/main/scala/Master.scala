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


case class CreateTopology()
case class StartGossip(gossipMessage:String)
case class StartPushSum()
case class STOPGossip()
case class STOPPushSum()

object Master {
	
	def props(acsys: ActorSystem, numNodes: Int, topology: String, algorithm:String):Props =
    Props(classOf[Master], acsys, numNodes, topology, algorithm)
}



class Master(acsys: ActorSystem, numNodes: Int, topology: String, algorithm:String) extends Actor{

	
	var ActorsList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
	var dimension:Int=0
	var ActorsIn3DGrid = Array.ofDim[ActorRef](dimension, dimension, dimension)
	

	def receive ={
		case CreateTopology()=> setUpNodes(numNodes,topology)
		
		case StartGossip(gossipMessage)=> 
			val r = (scala.util.Random).nextInt(ActorsList.length)
			println("\n"+"Start gossip from " + r +"th node in the ActorsList!" +"\n")
			ActorsList(r) ! BeginGossip(gossipMessage)	

		case StartPushSum()=>			
			val r = (scala.util.Random).nextInt(ActorsList.length)
			println("\nStart pushsum from " + r + "th node in the ActorsList! \n" )
			ActorsList(r) ! BeginPushSum()
			

		case STOPGossip() =>
			println ("messageCount exceeded the limit.")
			context.system.shutdown()

		case STOPPushSum() =>
			println ("Converging less than 10^-10 in three consecutive rounds.")
			context.system.shutdown()

	}


	def setUpNodes(numNodes:Int, topology:String):Int = {
		//Actors arranged in Line: Linear array
		if((topology == "Line")||(topology=="FullNetwork")){
			for (i<-0 until numNodes){
				var index:Int =i
				val acref =  context.actorOf(Props(classOf[Worker], i) )
				ActorsList+=acref
			}
		}


		//Actors arranged in 3D Grid: 3D array
		if ((topology == "3DGrid")||(topology=="Imperfect3DGrid")){
			println("\nTopology: "+ topology)
			dimension = scala.math.ceil(scala.math.cbrt(numNodes)).toInt
			
			ActorsIn3DGrid = Array.ofDim[ActorRef](dimension, dimension, dimension)
			val newnumNodes:Int= scala.math.pow(dimension, 3).toInt
			//creating actors
			for (i<-0 until newnumNodes){
				var index:Int =i
				val acref =  context.actorOf(Props(classOf[Worker], i) )
				ActorsList+=acref
			}
			println("\nNew number of nodes: " + ActorsList.length)
			println("\n"+ActorsList)			
			println("\nDimension of the Grid: " + dimension+ "*" +dimension+ "*"+dimension)
			

			//arranging actors into 3DGrid
			var x:Int =0			
			for (i<-0 until dimension){					
					for (j<-0 until dimension){
						for (k<-0 until dimension){
							//println("\ni:"+i+" j:"+j+" k:"+k)
							ActorsIn3DGrid(i)(j)(k)= ActorsList(x)
							//println(ActorsList(x))
							x +=1
						}

					}
				}

			println("-----------------------------------------------------")
		

		}		
		

		//Creating neighbours list according to topology 
		topology match{
			case "FullNetwork"=>{
				for (i<-0 until numNodes){
					var neighboursList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
					for (j<-0 until numNodes){
						if(j!=i){
							neighboursList+= ActorsList(j)
						}
					}
					
					ActorsList(i) ! SetYourNeighboursList(neighboursList)					
				}
			}

			case "Line" => {
				for (i <- 0 until numNodes) {
		          var neighboursList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
		          if(i == 0) {
		            neighboursList += ActorsList(i + 1)
		          }
		          else if(i == (numNodes - 1)) {
		            neighboursList += ActorsList(i - 1)
		          }
		          else {
		            neighboursList += ActorsList(i - 1)
		            neighboursList += ActorsList(i + 1)
		          }

		          ActorsList(i) ! SetYourNeighboursList(neighboursList)
				}				
			}
			case "3DGrid" => {
				makeNeighboursList(ActorsIn3DGrid, dimension)				
			}



			//case Imperfect3DGrid
		}

		return 0
	}

	def makeNeighboursList(ActorsIn3DGrid:Array[Array[Array[ActorRef]]], dimension:Int){
				
		for (i<-0 until dimension){
			for (j<-0 until dimension){
				for (k<-0 until dimension){
					var includeLeft:Boolean = true
					var includeRight:Boolean = true
					var includeBottom:Boolean = true
					var includeTop:Boolean = true
					var includeFront:Boolean = true
					var includeBack:Boolean = true					

					var neighboursList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]

					if(i==0){includeLeft = false}
					if(i==(dimension-1)){includeRight = false}
					if(j==0){includeBottom = false}
					if(j==(dimension-1)){includeTop = false}
					if(k==0){includeFront = false}
					if(k==(dimension-1)){includeBack = false}

					if(includeLeft){neighboursList += ActorsIn3DGrid((i-1))(j)(k)}
					if(includeRight){neighboursList += ActorsIn3DGrid((i+1))(j)(k)}
					if(includeBottom){neighboursList += ActorsIn3DGrid(i)((j-1))(k)}
					if(includeTop){neighboursList += ActorsIn3DGrid(i)((j+1))(k)}
					if(includeFront){neighboursList += ActorsIn3DGrid(i)(j)((k-1))}
					if(includeBack){neighboursList += ActorsIn3DGrid(i)(j)((k+1))}

					println("\nActor at node, i:"+i+" j:"+j+" k:"+k)
					println("My neighboursList is: " + neighboursList)
					for (z<-0 until neighboursList.length){
							for (i<-0 until dimension){
								for (j<-0 until dimension){
									for (k<-0 until dimension){
										if(ActorsIn3DGrid(i)(j)(k) == neighboursList(z))
											println("Neighbour's Index: i:"+i+" j:"+j+" k:"+k)
											}}}
					}

					ActorsIn3DGrid(i)(j)(k) ! SetYourNeighboursList(neighboursList)

				}

			}
		}

		
	}



}

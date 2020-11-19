import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
abstract class HNode(val ident:String,val parent:Option[HNode],val fields:mutable.HashMap[String,Type]) {
  def this(ident:String,field:mutable.HashMap[String,Type]) {
    this(ident,None,field)
  }
  def this(ident:String, parent:Option[HNode]) {
    this(ident,parent,new mutable.HashMap[String,Type]())
  }
  def getVar(ident:String): Option[Type] = {
    Some(this.fields.getOrElse(ident, parent.getOrElse(return None).getVar(ident).getOrElse(return None)))
  }
  def getVarHere(ident:String): Option[Type] = {
    this.fields.get(ident)
  }
  def findClass(ident:String):Option[CNode]
  def findMethod(ident:String,args:Seq[Type]):Option[MNode]
  def getThis: CNode

  def cloneSingle():HNode
}
class CNode(ident:String, parent:Option[HNode],val clazz:Clazz, field:mutable.HashMap[String,Type]) extends HNode(ident,parent,field){

  val methods:ArrayBuffer[MNode] = new ArrayBuffer[MNode]()
  val children:ArrayBuffer[CNode] = new ArrayBuffer[CNode]()

  clazz.methods.foreach(p => {
    this.methods += new MNode(p.ident,Some(this),p.tipe,p.params,p)
  })

  clazz.varDecs.foreach(p => this.fields.put(p.ident,p.tipe))

  private def this(ident:String,fields:mutable.HashMap[String,Type]) {
    this(ident,None,null,fields)
  }

  def containsClass(ident:String):Boolean = {
    if(this.ident == ident)
      true
    else if(children.nonEmpty)
      children.exists(_.containsClass(ident))
    else
      false
  }
  def findClass(ident:String):Option[CNode] = {
    if(this.ident == ident)
      Some(this)
    else if(children.nonEmpty)
      children.find(p => p.findClass(ident).nonEmpty)
    else None
  }
  def isChildOf(ident:String):Boolean = {
    if(parent.isEmpty)
      false
    else if(parent.get.ident== ident)
      true
    else if(parent.get.isInstanceOf[CNode])
      parent.asInstanceOf[CNode].isChildOf(ident)
    else
      throw new HierarchyError
  }

  override def findMethod(ident: String, args:Seq[Type]): Option[MNode] = {
    methods.find(p => p.ident ==ident && p.parameters.types == args)

  }

  override def cloneSingle(): HNode = {
    new CNode(this.ident, None, null,fields)
  }

  override def getThis: CNode = this
}
class MNode(ident:String, parent:Option[HNode],var rType:Type, val parameters: Parameters,field:mutable.HashMap[String,Type]) extends HNode(ident,parent,field) {
  def this(ident:String,parent:Option[HNode],rType:Type,parameters:Parameters, mDec:MethodDec) {
    this(ident,parent,rType,parameters)
    mDec.varDecs.foreach(p => fields.put(p.ident,p.tipe))

  }
  def this(ident:String,parent:Option[HNode],rType: Type, parameters:Parameters) {
    this(ident,parent,rType,parameters,new mutable.HashMap[String,Type]())
  }


  override def findClass(ident: String): Option[CNode] = None

  override def findMethod(ident: String, args:Seq[Type]): Option[MNode] = None

  override def cloneSingle(): HNode = {
    new MNode(this.ident,None,this.rType,parameters,this.fields)
  }

  override def getThis: CNode = {
    if(parent.nonEmpty)
      parent.get match {
        case e:CNode => e
        case _ => throw new HierarchyError
      }
    else
      throw new HierarchyError
  }
}

class LNode(ident:String, val lambdaI:LambdaI) extends HNode(ident,None) {
  val method:MNode = new MNode(lambdaI.ident, Some(this), lambdaI.mTipe,lambdaI.params)

  override def findClass(ident: String): Option[CNode] = None

  override def findMethod(ident: String, args:Seq[Type]): Option[MNode] = Some(method)

  override def cloneSingle(): HNode = {
    new LNode(ident,null)
  }

  override def getThis: CNode = {
    throw new HierarchyError
  }
}

object Object extends CNode("java/lang/Object", None,null,new mutable.HashMap[String,Type]())

class Hierarchy {
  val head:CNode = Object
  val lambdas = new ArrayBuffer[LNode]()

  def addClass(clazz:Clazz): Unit = {
    val parent = findClass(clazz.ident)
    parent.getOrElse(throw new NodeNotFoundException).children += new CNode(clazz.ident,parent,clazz,new mutable.HashMap[String,Type]())
  }
  def addLambda(lambda:LambdaI): Unit = {
    lambdas += new LNode(lambda.ident, lambda)
  }
  def containsClass(clazz:CNode): Boolean = {
    containsClass(clazz.ident)
  }
  def containsClass(ident:String):Boolean = {
    head.containsClass(ident)
  }
  def findClass(ident:String):Option[CNode] = {
    head.findClass(ident)
  }
  def findLambda(ident:String):Option[LNode] = {
    lambdas.find(p => p.ident == ident)
  }
  def containsLambda(ident:String):Boolean = {
    lambdas.exists(_.ident == ident)
  }
}

class NodeNotFoundException extends Exception
class HierarchyError extends Exception
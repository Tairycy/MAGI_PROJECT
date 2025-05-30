package utils.bus.AxiLite
/******************************************************************************
 *  This file describes the AxiLite4 interface
 *
 *   _________________________________________________________________________
 *  | Global | Write Data | Write Addr | Write Resp | Read Data  | Read Addr  |
 *  |   -    |    w       |    aw      |      b     |     r      |     ar     |
 *  |-------------------------------------------------------------------------|
 *  |  aclk  |  wvalid    |  awvalid   |  bvalid    |  rvalid    |  arvalid   |
 *  |  arstn |  wready    |  awready   |  bready    |  rready    |  arready   |
 *  |        |  wdata     |  awaddr    |  bresp     |  rdata     |  araddr    |
 *  |        |  wstrb     |  awprot    |            |  rresp     |  arprot    |
 *  |________|____________|____________|____________|____________|____________|
 */
import spinal.core._
import spinal.lib._


/**
 * Definition of the constants used by the AXI Lite bus
 */
object AxiLite4 {

    def apply(addressWidth : Int,
              dataWidth    : Int) = new AxiLite4(AxiLite4Config(
        addressWidth = addressWidth,
        dataWidth = dataWidth
    ))

    /**
     * Read Write response
     */
    object resp{
        def apply() = Bits(2 bits)
        def OKAY   = B"00" // Normal access success
        def EXOKAY = B"01" // Exclusive access okay
        def SLVERR = B"10" // Slave error
        def DECERR = B"11" // Decode error
    }

    /**
     * Access permissions
     */
    object prot{
        def apply() = Bits(3 bits)
        def UNPRIVILEGED_ACCESS = B"000"
        def PRIVILEGED_ACCESS   = B"001"
        def SECURE_ACCESS       = B"000"
        def NON_SECURE_ACCESS   = B"010"
        def DATA_ACCESS         = B"000"
        def INSTRUCTION_ACCESS  = B"100"
    }
}

/**
 * Configuration class for the Axi Lite bus
 * @param addressWidth Width of the address bus
 * @param dataWidth    Width of the data bus
 */
case class AxiLite4Config(addressWidth : Int,
                          dataWidth    : Int,

                          readIssuingCapability     : Int = -1,
                          writeIssuingCapability    : Int = -1,
                          combinedIssuingCapability : Int = -1,
                          readDataReorderingDepth   : Int = -1){
    def bytePerWord: Int = dataWidth/8
    def addressType: UInt = UInt(addressWidth bits)
    def dataType: Bits = Bits(dataWidth bits)

    require(dataWidth == 32 || dataWidth == 64, "Data width must be 32 or 64")
}

/**
 * Definition of the Write/Read address channel
 * @param config Axi Lite configuration class
 */
case class AxiLite4Ax(config: AxiLite4Config) extends Bundle{
    val addr = UInt(config.addressWidth bits)
    val prot = Bits(3 bits)


    import AxiLite4.prot._

    def setUnprivileged() : Unit = prot := UNPRIVILEGED_ACCESS | SECURE_ACCESS | DATA_ACCESS
    def setPermissions(permission : Bits) : Unit = prot := permission
}

/**
 * Definition of the Write data channel
 * @param config Axi Lite configuration class
 */
case class AxiLite4W(config: AxiLite4Config) extends Bundle {
    val data = Bits(config.dataWidth bits)
    val strb = Bits(config.dataWidth / 8 bits)

    def setStrb() : Unit = strb := (1 << widthOf(strb))-1
    def setStrb(bytesLane : Bits) : Unit = strb := bytesLane
}

/**
 * Definition of the Write response channel
 * @param config Axi Lite configuration class
 */
case class AxiLite4B(config: AxiLite4Config) extends Bundle {
    val resp = Bits(2 bits)

    import AxiLite4.resp._

    def setOKAY()   : Unit = resp := OKAY
    def setEXOKAY() : Unit = resp := EXOKAY
    def setSLVERR() : Unit = resp := SLVERR
    def setDECERR() : Unit = resp := DECERR
    def isOKAY()    : Unit = resp === OKAY
    def isEXOKAY()  : Unit = resp === EXOKAY
    def isSLVERR()  : Unit = resp === SLVERR
    def isDECERR()  : Unit = resp === DECERR
}

/** Companion object to create hard-wired AXI responses. */
object AxiLite4B {
    def okay(config: AxiLite4Config) = { val resp = new AxiLite4B(config); resp.setOKAY(); resp }
    def exclusiveOkay(config: AxiLite4Config) = { val resp = new AxiLite4B(config); resp.setEXOKAY(); resp }
    def slaveError(config: AxiLite4Config) = { val resp = new AxiLite4B(config); resp.setSLVERR(); resp }
    def decodeError(config: AxiLite4Config) = { val resp = new AxiLite4B(config); resp.setDECERR(); resp }
}

/**
 * Definition of the Read data channel
 * @param config Axi Lite configuration class
 */
case class AxiLite4R(config: AxiLite4Config) extends Bundle {
    val data = Bits(config.dataWidth bits)
    val resp = Bits(2 bits)

    import AxiLite4.resp._

    def setOKAY()   : Unit = resp := OKAY
    def setEXOKAY() : Unit = resp := EXOKAY
    def setSLVERR() : Unit = resp := SLVERR
    def setDECERR() : Unit = resp := DECERR
    def isOKAY()    : Unit = resp === OKAY
    def isEXOKAY()  : Unit = resp === EXOKAY
    def isSLVERR()  : Unit = resp === SLVERR
    def isDECERR()  : Unit = resp === DECERR
}


/**
 * Axi Lite interface definition
 * @param config Axi Lite configuration class
 */
case class AxiLite4(config: AxiLite4Config) extends Bundle with IMasterSlave {

    val aw = Stream(AxiLite4Ax(config))
    val w  = Stream(AxiLite4W(config))
    val b  = Stream(AxiLite4B(config))
    val ar = Stream(AxiLite4Ax(config))
    val r  = Stream(AxiLite4R(config))

    //Because aw w b ar r are ... very lazy
    def writeCmd  = aw
    def writeData = w
    def writeRsp  = b
    def readCmd   = ar
    def readRsp   = r


    def >> (that : AxiLite4) : Unit = {
        assert(that.config == this.config)
        this.writeCmd  >> that.writeCmd
        this.writeData >> that.writeData
        this.writeRsp  << that.writeRsp

        this.readCmd  >> that.readCmd
        this.readRsp << that.readRsp
    }

    def <<(that : AxiLite4) : Unit = that >> this

    def |>> (that : AxiLite4) : Unit = {
        assert(that.config == this.config)
        this.aw >> that.aw
        this.w >> that.w
        this.ar >> that.ar
        this.b << that.b
        this.r << that.r
        that.aw.addr.removeAssignments()
        that.ar.addr.removeAssignments()
        that.aw.addr := this.aw.addr.resized
        that.ar.addr := this.ar.addr.resized
    }

    def |<< (that: AxiLite4): Unit = {
        assert(that.config == this.config)
        this.aw << that.aw
        this.w << that.w
        this.ar << that.ar
        this.b >> that.b
        this.r >> that.r
        this.aw.addr.removeAssignments()
        this.ar.addr.removeAssignments()
        this.aw.addr := that.aw.addr.resized
        this.ar.addr := that.ar.addr.resized
    }

    override def asMaster(): Unit = {
        master(aw,w)
        slave(b)

        master(ar)
        slave(r)
    }
}

/**
 * AxiLite4 Rename all Channels
 */
object  AxiLite4SpecRenamer{
    def apply(that : AxiLite4): Unit ={
        def doIt() = {
            that.flatten.foreach((bt) => {
                bt.setName(bt.getName().replace("_payload_",""))
                bt.setName(bt.getName().replace("_valid","valid"))
                bt.setName(bt.getName().replace("_ready","ready"))
                if(bt.getName().startsWith("io_")) bt.setName(bt.getName().replaceFirst("io_",""))
            })
        }
        if(Component.current == that.component)
            that.component.addPrePopTask(() => {doIt()})
        else
            doIt()
    }
}
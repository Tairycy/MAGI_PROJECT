package magiRF.modules.DSP

import magiRF.modules.DSP.FFT.{FFTConfig, R2MDC, R2MDC_FFT}
import spinal.core._
import spinal.lib._
import spinal.core.sim._
import utils.bus.AxiLite.sim.AxiLite4Driver
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer}

import scala.util.Random


object R2MDC_FFTSimApp extends App{

    val fft_config = FFTConfig(12 exp, -11 exp, 64)

    SimConfig
        .withWave
        .allOptimisation
        .doSim(new R2MDC_FFT(fft_config)){ dut =>
            dut.clockDomain.forkStimulus(5)
            dut.io.mode #= true
            dut.io.raw_data_iq.valid #= false
            dut.io.raw_data_iq.cha_i #= 0
            dut.io.raw_data_iq.cha_q #= 0
            dut.clockDomain.waitSampling(20)
            for (cnt <- 0 until 2){
                var idx = 0
                while (idx != 64){
                    dut.io.raw_data_iq.valid.randomize()
                    dut.io.raw_data_iq.cha_i #= idx << 11
                    dut.io.raw_data_iq.cha_q #= 0
                    dut.clockDomain.waitSampling(1)
                    idx = idx + dut.io.raw_data_iq.valid.toBoolean.toInt
                }
                dut.io.raw_data_iq.valid #= false
                dut.clockDomain.waitSamplingWhere(dut.io.raw_data_iq.ready.toBoolean)
            }
            dut.io.raw_data_iq.valid #= false
            dut.clockDomain.waitSampling(256)
        }
}

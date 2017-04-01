/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tigase;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.profile.WinPerfAsmProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import tigase.server.Packet;
import tigase.server.PacketFilterIfc;
import tigase.server.QueueType;
import tigase.server.filters.PacketCounter;
import tigase.server.filters.PacketCounterOld;
import tigase.stats.StatisticsList;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class MyBenchmark {

	public static void main(String[] args) throws RunnerException {

		final TimeValue seconds = TimeValue.seconds(10);
		final int iterations = 20;
//		final TimeValue seconds = TimeValue.seconds(3);
//		final int iterations = 2;


		Options opt = new OptionsBuilder()
				// Specify which benchmarks to run.
				.include(MyBenchmark.class.getName() + ".*")
				.mode (Mode.AverageTime)
//				.mode(Mode.Throughput)
//				.timeUnit(TimeUnit.SECONDS)
				.timeUnit(TimeUnit.MICROSECONDS)
				.warmupTime(seconds)
				.warmupIterations(iterations)
				.measurementTime(seconds)
				.measurementIterations(iterations)
//				.threads(8)
				.threads(1)
				.forks(1)
				.shouldFailOnError(true)
				.shouldDoGC(true)
				.timeout(TimeValue.minutes(1))
//				.addProfiler(StackProfiler.class)
				.build();

		new Runner(opt).run();


//		PacketCounter pcNewDetailedOn = new PacketCounter(true);
//		final PseudoRandomPacketGenerator generator = new PseudoRandomPacketGenerator();
//		Queue<Packet> packets = generator.generateSemiRandomPacketQueue(50L * 1000 * 10, 5);
//
//		Packet p = null;
//		while ((p = packets.poll()) != null) {
//			pcNewDetailedOn.filter(p);
//		}
//
//		pcNewDetailedOn.init("new", QueueType.IN_QUEUE);
//
//		final StatisticsList statRecords = new StatisticsList(java.util.logging.Level.ALL);
//		pcNewDetailedOn.getStatistics(statRecords);
//		statRecords.forEach(System.out::println);

	}

	@Benchmark
	public void benchmarkOld(BenchmarkState state, Blackhole bh) {
		process(state, bh, state.pcOld);
	}

	private void process(BenchmarkState state, Blackhole bh, PacketFilterIfc pcOld) {
		Packet p;
//		if ((p = state.packets.poll()) != null) {
//			bh.consume(pcOld.filter(p));
//		}
		for (int i = 0 ; i< 1000 ; i++ ) {
			if ((p = state.packets.poll()) != null) {
				bh.consume(pcOld.filter(p));
			}
		}
	}

	@Benchmark
	public void benchmarkNewDetailedOn(BenchmarkState state, Blackhole bh) {
		process(state, bh, state.pcNewDetailedOn);
	}

	@Benchmark
	public void benchmarkNewDetailedOff(BenchmarkState state, Blackhole bh) {
		process(state, bh, state.pcNewDetailedOff);
	}

	//	@State(Scope.Thread)
	@State(Scope.Benchmark)
	public static class BenchmarkState {

		long limit = 50L * 1000 * 10;

		StatisticsList sl = new StatisticsList(java.util.logging.Level.ALL);

		Queue<Packet> packets = new ArrayDeque<>((int) limit);
		PacketCounter pcNewDetailedOn = new PacketCounter(true);
		PacketCounter pcNewDetailedOff = new PacketCounter(false);
		PacketFilterIfc pcOld = new PacketCounterOld();



		@Setup(Level.Iteration)
		public void initialize() {

			final PseudoRandomPacketGenerator generator = new PseudoRandomPacketGenerator();
			packets = generator.generateSemiRandomPacketQueue(limit, 2);
//			packets = generator.generateSemiRandomPacketQueue(new String[]{"message"}, new String[]{}, limit, 2);
//			System.out.println();
//			System.out.println("packets:");
//			packets.forEach(System.out::println);
//			System.out.println();
//			System.out.printf("generated, sum: %s, head: %s \n", generator.sum, packets.peek());
		}

		@TearDown(Level.Trial)
		public void tearDown() {

			System.out.println();
			System.out.println();
			System.out.println();

			pcNewDetailedOn.init("counter-new-detailed-on", QueueType.IN_QUEUE);
			pcNewDetailedOn.getStatistics(sl);
			pcNewDetailedOff.init("counter-new-detailed-off", QueueType.IN_QUEUE);
			pcNewDetailedOff.getStatistics(sl);
			pcOld.init("counter-old", QueueType.IN_QUEUE);
			pcOld.getStatistics(sl);
			sl.forEach(System.out::println);

			System.out.println();
			System.out.println();
		}

	}

}

package mallocator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class Mallocator
{
	static final String IFNAME_M_DEF = "Minput.data";
	static final String IFNAME_P_DEF = "Pinput.data";
	static final String[] OFNAMES = { "FFoutput.data", "BFoutput.data", "WFoutput.data" };
	
	private static boolean QUIET = false;
	private static boolean DEBUG;
	
	private String[] outfilenames;
	
	private final int MEM_SLOT_CNT;
	private final int PROCESS_CNT;
	
	private Queue<PCB> processQueue;
	private MSB[] Memory;
	
	
	public Mallocator(Scanner minput)
	{
		
		if (!QUIET) System.out.println(" -------- Constructing MEM ALLOCATOR --------");
		outfilenames = OFNAMES;
		processQueue = new java.util.LinkedList<PCB>();
		
		/* Reads information about available memory space */
		MEM_SLOT_CNT = minput.nextInt();
		Memory = new MSB[MEM_SLOT_CNT];
		for (int i = 0; i < MEM_SLOT_CNT; i++)
			Memory[i] = new MSB(
					i,
					minput.nextInt(),	// memory slot start index
					minput.nextInt()	// memory slot end index
				);
		
		/* Reads process information */
		PROCESS_CNT = minput.nextInt();
		for (int i = 1; i <= PROCESS_CNT; i++)
			processQueue.add( new PCB(
					minput.nextInt(),	// Process id
					minput.nextInt()));	// Process size
		
		minput.close();
		//pinput.close();
		if (!QUIET) System.out.println("\n -------- MEM ALLOCATOR Constructed --------\n\n");
	}
	
	
	
	public void firstFit() throws FileNotFoundException 
	{
		if (!QUIET) System.out.println("     ~ First Fit ~");
		
		MSB[] memory = new MSB[this.Memory.length];
		System.arraycopy(this.Memory, 0, memory, 0, this.Memory.length);
		PrintWriter outfile = new PrintWriter(outfilenames[0]);
		Queue<PCB> processQueue = new LinkedList<PCB>(this.processQueue);
		Queue<PCB> rejectQueue = new LinkedList<PCB>();
		
		while (!processQueue.isEmpty()) 
		{
			boolean added = false;
			// Iterate across all memory slots
			for (int i = 0; i < memory.length && !added; i++) 
			{
				// Choose the first one with sufficient space
				if (processQueue.peek().size <= memory[i].getSpace()) {
					memory[i].add(processQueue.poll());
					added = true;
				}
			}
			if (!added) rejectQueue.add(processQueue.poll());
		}
		
		for (MSB slot : memory) 
			for (PCB process : slot.contents.values())
				outfile.println(process.getstartix() + "    " + process.getendix() + "    " + process.id);
		outfile.print("-" + (rejectQueue.isEmpty()? "0" : ""));
		for (PCB process : rejectQueue)
			outfile.print(process.id + ",");
		
		outfile.close();
	}
	
	
	
	public void bestFit() throws FileNotFoundException
	{
		if (!QUIET) System.out.println("     ~ Best Fit ~");
		
		MSB[] memory = new MSB[this.Memory.length];
		System.arraycopy(this.Memory, 0, memory, 0, this.Memory.length);
		PrintWriter outfile = new PrintWriter(outfilenames[1]);
		Queue<PCB> processQueue = new LinkedList<PCB>(this.processQueue);
		Queue<PCB> rejectQueue = new LinkedList<PCB>();
		
		while (!processQueue.isEmpty()) {
			int smfitix = -1;	// Index of smallest mem slot large enough to fit process
			int smfitsz = Integer.MAX_VALUE;	// Size of smallest mem slot large enough to fit process
			for (int i = 0; i < memory.length; i++) {
				int slotSpace = memory[i].getSpace();
				if (processQueue.peek().size <= slotSpace
						&& slotSpace < smfitsz) {
					smfitsz = slotSpace;
					smfitix = i;
				}
			}
			
			if (smfitix == -1)
				rejectQueue.add(processQueue.poll());
			else
				memory[smfitix].add(processQueue.poll());
		}
		
		for (MSB slot : memory) 
			for (PCB process : slot.contents.values())
				outfile.println(process.getstartix() + "    " + process.getendix() + "    " + process.id);
		outfile.print("-" + (rejectQueue.isEmpty()? "0" : ""));
		for (PCB process : rejectQueue)
			outfile.print(process.id + ",");
		
		outfile.close();
	}
	
	
	
	public void worstFit() throws FileNotFoundException
	{
		if (!QUIET) System.out.println("     ~ Worst Fit ~");
		
		MSB[] memory = new MSB[this.Memory.length];
		System.arraycopy(this.Memory, 0, memory, 0, this.Memory.length);
		PrintWriter outfile = new PrintWriter(outfilenames[2]);
		Queue<PCB> processQueue = new LinkedList<PCB>(this.processQueue);
		Queue<PCB> rejectQueue = new LinkedList<PCB>();
		
		while (!processQueue.isEmpty()) {
			int smfitix = -1;	// Index of smallest mem slot large enough to fit process
			int smfitsz = Integer.MAX_VALUE;	// Size of smallest mem slot large enough to fit process
			for (int i = 0; i < memory.length; i++) {
				int slotSpace = memory[i].getSpace();
				if (processQueue.peek().size <= slotSpace
						&& slotSpace > smfitsz) {
					smfitsz = slotSpace;
					smfitix = i;
				}
			}
			
			if (smfitix == -1)
				rejectQueue.add(processQueue.poll());
			else
				memory[smfitix].add(processQueue.poll());
		}
		
		for (MSB slot : memory) 
			for (PCB process : slot.contents.values())
				outfile.println(process.getstartix() + "    " + process.getendix() + "    " + process.id);
		outfile.print("-" + (rejectQueue.isEmpty()? "0" : ""));
		for (PCB process : rejectQueue)
			outfile.print(process.id + ",");
		
		outfile.close();
	}
	
	
	
	public static void main(String[] args)
	{
		String memfilename = IFNAME_M_DEF;
		for (int i = 1; i <= args.length; i++) 
		{
			/* Parse Args: suppress display output */
			if (args[i-1].matches("-s") || args[i-1].matches("-q")) {
				System.out.println("   ~ quiet mode ~");
				Mallocator.QUIET = true;
			}
			/* Parse Args: force debug mode */
			if (args[i-1].matches("-d")) {
				System.out.println("   ~ debug mode ~");
				Mallocator.DEBUG = true;
				Mallocator.QUIET = false;
			}
			/* Parse Args: set new mem input file  */
			if (args[i-1].matches("-mf"))
				memfilename = args[i];
		}
		
		
		Scanner mfs;
		mfs = null;
		
		/* Reads Mem file */
		try {
			mfs = new Scanner( new File(memfilename) );
		} catch (IOException e) {
			System.out.println("Mem Allocation error: cannot find input file \"" + memfilename + "\" in current directory!");
			e.printStackTrace();
		}
		
		/* Invoke Mem Allocation program */
		try {
			Mallocator m = new Mallocator(mfs);
			if (!QUIET) System.out.println(" ---------- MEM ALLOCATOR Execution ----------\n");
			
			try {
				m.firstFit();
				m.bestFit();
				m.worstFit();
			} catch (FileNotFoundException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			
			if (!QUIET) System.out.println("\n ---------- MEM ALLOCATOR Execution Complete ---------- \n\n");
		} catch (InputMismatchException e) {
			System.out.println("Mem Allocation error: illegal format of input files!");
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Represents a single block of memory.
	 */
	private class MSB {
		final public int id;
		final public int size;
		final public int startIX;
		final public int endIX;
		
		private int nextIX;
		private int space;
		private HashMap<Integer,PCB> contents;
		
		MSB(int id, int startIX, int endIX) {
			this.id = id;
			this.size = space = endIX - startIX;
			this.startIX = nextIX = startIX;
			this.endIX = endIX;
			
			contents = new java.util.HashMap<Integer,PCB>();
		}
		
		public int getSpace() {
			return space;
		}
		
		/** 
		 * Load a process into this memory block.
		 * PRECONDITION  this memory block has enough memory to store the process.
		 */
		public void add(PCB process) {
			if (process == null) return;
			if (process.size > space)
				throw new IllegalArgumentException("Error adding process p" + process.id 
						+ " to block m" + this.id + ": insufficient space!" 
						+ "(" + process.size + "/" + space + ")");
			contents.put(process.id, process);
			process.setstartix(nextIX);
			nextIX += process.size;
			space -= process.size;
		}
		
		public void remove(int pid) {
			if (!contents.containsKey(pid))
				throw new IllegalArgumentException("Error removing process p" + pid
						+ " from block m" + this.id + ": process not found!");
			PCB process = contents.remove(pid);
			nextIX -= process.size;
			space += process.size;
		}
	}
	
	
	/**
	 * Represents a single process.
	 */
	private class PCB 
	{
		final public int id;
		final public int size;
		private int startix;
		
		/**
		 * Create a new process with the specified information.
		 * 
		 * @param id			Identifying number
		 * @param arriveTime	Time at which the process becomes ready for execution
		 * @param burstTime		Amount of cpu time required to finish process
		 * @param priority 		the process' execution priority
		 */
		public PCB (int id, int size)
		{
			this.id = id;
			this.size = size;
		}
		
		public int getstartix() {
			return startix;
		}
		public int getendix() {
			return startix + size;
		}
		public void setstartix(int index) {
			startix += index;
		}
		
	}
}

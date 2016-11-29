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

public class Mallocator implements Runnable
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
	
	
	public Mallocator(Scanner minput, Scanner pinput)
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
		PROCESS_CNT = pinput.nextInt();
		for (int i = 1; i <= PROCESS_CNT; i++)
			processQueue.add( new PCB(
					pinput.nextInt(),	// Process id
					pinput.nextInt()));	// Process size
		
		minput.close();
		pinput.close();
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
		{
			//TODO: implement file output
		}
		
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
			for (int i = 0; i < Memory.length; i++) {
				int slotSpace = Memory[i].getSpace();
				if (processQueue.peek().size <= slotSpace
						&& slotSpace < smfitsz) {
					smfitsz = slotSpace;
					smfitix = i;
				}
			}
			
			if (smfitix != -1)
				Memory[smfitix].add(processQueue.poll());
		}
		
		
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
			for (int i = 0; i < Memory.length; i++) {
				int slotSpace = Memory[i].getSpace();
				if (processQueue.peek().size <= slotSpace
						&& slotSpace > smfitsz) {
					smfitsz = slotSpace;
					smfitix = i;
				}
			}
			
			if (smfitix != -1)
				Memory[smfitix].add(processQueue.poll());
		}
		
		outfile.close();
	}
	
	
	/**
	 *  Begins execution of this Allocator.
	 */
	public void run() {
		if (!QUIET) System.out.println(" ---------- MEM ALLOCATOR Execution ----------\n");
		
		try {
			firstFit();
			bestFit();
			worstFit();
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		if (!QUIET) System.out.println("\n ---------- MEM ALLOCATOR Execution Complete ---------- \n\n");
	}
	
	
	
	
	public static void main(String[] args)
	{
		String memfilename = IFNAME_M_DEF;
		String prcfilename = IFNAME_P_DEF;
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
			/* Parse Args: set new process input file */
			if (args[i-1].matches("-pf"))
				prcfilename = args[i];
		}
		
		
		Scanner mfs, pfs;
		mfs = pfs = null;
		
		/* Reads Mem file */
		try {
			mfs = new Scanner( new File(memfilename) );
		} catch (IOException e) {
			System.out.println("Mem Allocation error: cannot find input file \"" + memfilename + "\" in current directory!");
			e.printStackTrace();
		}
		
		/* Reads Process file */
		try {
			pfs = new Scanner( new File(prcfilename) );
		} catch (IOException e) {
			System.out.println("Mem Allocation error: cannot find input file \"" + prcfilename + "\" in current directory!");
			e.printStackTrace();
		}
		
		/* Invoke Mem Allocation program */
		try {
			Mallocator m = new Mallocator(mfs, pfs);
			m.run();
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
		
		private int space;
		private HashMap<Integer,PCB> contents;
		
		MSB(int id, int startIX, int endIX) {
			this.id = id;
			this.size = space = endIX - startIX;
			this.startIX = startIX;
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
			if (process.size >= space)
				throw new IllegalArgumentException("Error adding process p" + process.id 
						+ " to block m" + this.id + ": insufficient space!" 
						+ "(" + process.size + "/" + space + ")");
			contents.put(process.id, process);
			space -= process.size;
		}
		
		public void remove(int pid) {
			if (!contents.containsKey(pid))
				throw new IllegalArgumentException("Error removing process p" + pid
						+ " from block m" + this.id + ": process not found!");
			space += contents.remove(pid).size;
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
		public void set(int index) {
			startix += index;
		}
		
	}
}

// Import necessary libraries for input/output and data structures
import java.io.*;
import java.util.*;

// Define a class named CPUScheduler
public class CPUScheduler {

    // Entry point of the program
    public static void main(String[] args) {

        // Create queues for different scheduling algorithms and available memory
        Queue<PCB> jobQueue = new LinkedList<>();
        Queue<PCB> readyQueue = new LinkedList<>();
        Queue<PCB> readyQueueFCFS = new LinkedList<>();
        Queue<PCB> readyQueueSJF = new LinkedList<>();
        Queue<PCB> readyQueueRR3 = new LinkedList<>();
        Queue<PCB> readyQueueRR5 = new LinkedList<>();

        // Available memory is set to 8192 MB
        var ref = new Object() {
            int availableMemory = 8192;
        };

        // Define a custom exception for memory size exceeding
        class MemorySizeExceededException extends Exception {
            public MemorySizeExceededException(String message) {
                super(message);
            }
        }

        // Thread for reading from the file
        Thread fileThread = new Thread(() -> {
            try {
                // Read the file containing job information
            	BufferedReader br = new BufferedReader(new FileReader("/Users/raedalsaedi/eclipse-workspace/CSC 227 Project/bin/job.txt"));
                int counter = 0;
                String line;

                // Read each line from the file and process job information
                while ((line = br.readLine()) != null && counter < 30) {
                    String[] parts = line.split(",");

                    int id = Integer.parseInt(parts[0].trim());
                    int burstTime = Integer.parseInt(parts[1].trim());
                    int memory = Integer.parseInt(parts[2].trim());

                    // Check if memory size exceeds the limit
                    if (memory > 8192) {
                        throw new MemorySizeExceededException("Error: Maximum size of memory is 8192MB!!!");
                    }

                    // Set arrival time when creating PCB (Process Control Block)
                    PCB pcb = new PCB(id, burstTime, memory);

                    // Synchronize access to jobQueue and add the PCB
                    synchronized (jobQueue) {
                        jobQueue.add(pcb);
                        jobQueue.notifyAll();
                    }
                    counter++;
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MemorySizeExceededException e) {
                System.out.println(e.getMessage());
            }
        });

        // Thread for loading jobs to the ready queue
        Thread loadThread = new Thread(() -> {
            synchronized (jobQueue) {
                while (true) {
                    // Wait if jobQueue is empty or available memory is less than required
                    if (jobQueue.isEmpty() || ref.availableMemory < jobQueue.peek().memory) {
                        try {
                            jobQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    PCB pcb = jobQueue.poll();

                    // Clone the PCB for FCFS (First Come First Serve) and SJF (Shortest Job First)
                    PCB pcbForFCFS = new PCB(pcb.id, pcb.burstTime, pcb.memory);
                    PCB pcbForSJF = new PCB(pcb.id, pcb.burstTime, pcb.memory);

                    // Add cloned PCBs to their respective ready queues
                    readyQueueFCFS.add(pcbForFCFS);
                    readyQueueSJF.add(pcbForSJF);
                    readyQueueRR3.add(new PCB(pcb.id, pcb.burstTime, pcb.memory));
                    readyQueueRR5.add(new PCB(pcb.id, pcb.burstTime, pcb.memory));

                    

                    // Notify other threads waiting on jobQueue
                    jobQueue.notifyAll();
                }
            }
        });

        // Main thread for scheduling
        Thread scheduleThread = new Thread(() -> {

            // Call scheduling algorithms
            FCFS(readyQueueFCFS, jobQueue);
            SJF(readyQueueSJF);
            RR3(readyQueueRR3, 3);
            RR5(readyQueueRR5, 5);

            // Compare and print results
            Compare();

        });

        // Start threads
        fileThread.start();

        // Wait for fileThread to finish before starting loadThread
        try {
            fileThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadThread.start();
        scheduleThread.start();
    }

    // First Come First Serve scheduling algorithm
    public static void FCFS(Queue<PCB> readyQueue, Queue<PCB> jobQueue) {
        System.out.println("FCFS");
        int currentTime = 0;
        int totalTurnaroundTime = 0;
        int totalWaitingTime = 0;
        int start = 0;
        int readyQSize = readyQueue.size();

        while (true) {
            // Process PCBs in FCFS order
            if (!readyQueue.isEmpty()) {
                PCB pcb = readyQueue.poll();

                // Update times and print scheduling information
                int turnaroundTime = currentTime + pcb.burstTime;
                int waitingTime = currentTime;
                currentTime += pcb.burstTime;
                totalTurnaroundTime += turnaroundTime;
                totalWaitingTime += waitingTime;
                System.out.print("P" + pcb.id + " [" + start + ", " + currentTime + "]");
                System.out.print(" , ");
                start += pcb.burstTime;
            }
            // Print average turnaround and waiting times when all processes are done
            if (readyQueue.size() == 0 && jobQueue.isEmpty()) {
                System.out.print("\nAverage Turnaround Time= " + (double) totalTurnaroundTime / readyQSize);
                System.out.println("    Average Waiting Time= " + (double) totalWaitingTime / readyQSize);
                System.out.println("\n");
                PCB.TTFCFS = (double) totalTurnaroundTime / readyQSize;
                PCB.WTFCFS = (double) totalWaitingTime / readyQSize;
            }

            // Break if both jobQueue and readyQueue are empty
            if (jobQueue.isEmpty()) {
                if (readyQueue.isEmpty()) {
                    break;
                }
            }
        }
    }

    // Shortest Job First scheduling algorithm
    public static void SJF(Queue<PCB> readyQueue) {
        System.out.println("SJF");
        int currentTime = 0;
        int totalTurnaroundTime = 0;
        int totalWaitingTime = 0;
        int start = 0;

        // Convert readyQueue to a list and sort by burst time
        List<PCB> processes = new ArrayList<>(readyQueue);
        processes.sort(Comparator.comparingInt(p -> p.burstTime));
        int listSize = processes.size();

        // Process PCBs in SJF order
        while (!processes.isEmpty()) {
            PCB pcb = processes.remove(0); // Remove the first element from the sorted list
            int turnaroundTime = currentTime + pcb.burstTime;
            currentTime += pcb.burstTime;
            System.out.print("P" + pcb.id + " [" + start + ", " + currentTime + "]");
            System.out.print(" , ");
            start += pcb.burstTime;
            totalWaitingTime += start - pcb.burstTime; // Update totalWaitingTime correctly
            totalTurnaroundTime += turnaroundTime;
        }

        // Print average turnaround and waiting times
        System.out.print("\nAverage Turnaround Time= " + (double) totalTurnaroundTime / listSize);
        System.out.println("    Average Waiting Time= " + (double) totalWaitingTime / listSize);
        System.out.println("\n");
        PCB.TTSJF = (double) totalTurnaroundTime / listSize;
        PCB.WTSJF = (double) totalWaitingTime / listSize;
    }

    // Round Robin scheduling algorithm with time quantum of 3 units
    public static void RR3(Queue<PCB> readyQueue, int quantum) {
        System.out.println("RR3");
        int TurnaroundTimeForProcess = 0;
        int totalWaitingTime = 0;
        int readyQSize = readyQueue.size();
        int totalAroundTime = 0;
        int totalBurstTime = 0;
        int start = 0;
        int currentTime = 0;

        // Process PCBs in Round Robin with time quantum
        while (!readyQueue.isEmpty()) {
            PCB pcb = readyQueue.poll();
            int burstTime = Math.min(quantum, pcb.burstTime);
            currentTime += burstTime;
            totalBurstTime += burstTime;

            // Update times
            pcb.burstTime -= burstTime;
            TurnaroundTimeForProcess += burstTime;

            // Add PCB back to the readyQueue if it still has burst time left
            if (pcb.burstTime > 0) {
                System.out.print("P" + pcb.id + " [" + start + ", " + currentTime + "]");
                System.out.print(" , ");
                start += burstTime;
                readyQueue.add(pcb);
            } else {
                // PCB is done, update totalAroundTime
                System.out.print("P" + pcb.id + " [" + start + ", " + currentTime + "]");
                System.out.print(" , ");
                start += burstTime;
                totalAroundTime += TurnaroundTimeForProcess;
            }
        }

        // Calculate total waiting time
        totalWaitingTime = totalAroundTime - totalBurstTime;

        // Print average turnaround and waiting times
        System.out.print("\n Average Turnaround Time= " + (double) totalAroundTime / readyQSize);
        System.out.println("    Average Waiting Time= " + (double) totalWaitingTime / readyQSize);
        System.out.println("\n");
        PCB.TTRR3 = (double) totalAroundTime / readyQSize;
        PCB.WTRR3 = (double) totalWaitingTime / readyQSize;
    }

    // Round Robin scheduling algorithm with time quantum of 5 units
    public static void RR5(Queue<PCB> readyQueue, int quantum) {
        System.out.println("RR5");
        int TurnaroundTimeForProcess = 0;
        int totalWaitingTime = 0;
        int readyQSize = readyQueue.size();
        int totalAroundTime = 0;
        int totalBurstTime = 0;
        int start = 0;
        int currentTime = 0;

        // Process PCBs in Round Robin with time quantum
        while (!readyQueue.isEmpty()) {
            PCB pcb = readyQueue.poll();
            int burstTime = Math.min(quantum, pcb.burstTime);
            currentTime += burstTime;
            totalBurstTime += burstTime;

            // Update times
            pcb.burstTime -= burstTime;
            TurnaroundTimeForProcess += burstTime;

            // Add PCB back to the readyQueue if it still has burst time left
            if (pcb.burstTime > 0) {
                System.out.print("P" + pcb.id + " [" + start + ", " + currentTime + "]");
                System.out.print(" , ");
                start += burstTime;
                readyQueue.add(pcb);
            } else {
                // PCB is done, update totalAroundTime
                System.out.print("P" + pcb.id + " [" + start + ", " + currentTime + "]");
                System.out.print(" , ");
                start += burstTime;
                totalAroundTime += TurnaroundTimeForProcess;
            }
        }

        // Calculate total waiting time
        totalWaitingTime = totalAroundTime - totalBurstTime;

        // Print average turnaround and waiting times
        System.out.print("\n Average Turnaround Time= " + (double) totalAroundTime / readyQSize);
        System.out.print("    Average Waiting Time= " + (double) totalWaitingTime / readyQSize);
        PCB.TTRR5 = (double) totalAroundTime / readyQSize;
        PCB.WTRR5 = (double) totalWaitingTime / readyQSize;
    }

    // Compare results of different scheduling algorithms and print the best
    public static void Compare() {
        System.out.println("\n \nThe best Average TT : ");
        if (PCB.TTFCFS <= PCB.TTRR3 && PCB.TTFCFS <= PCB.TTRR5 && PCB.TTFCFS<= PCB.TTSJF)
			System.out.println("FCFS ");
		if(PCB.TTSJF<= PCB.TTRR3 && PCB.TTSJF<= PCB.TTRR5&& PCB.TTFCFS>= PCB.TTSJF)
			System.out.println("SJF ");
		if(PCB.TTFCFS>= PCB.TTRR3 && PCB.TTRR3<= PCB.TTRR5&& PCB.TTRR3<= PCB.TTSJF)
			System.out.println("RR3 ");
		if(PCB.TTRR5<= PCB.TTRR3 && PCB.TTRR5<= PCB.TTFCFS&& PCB.TTRR5<= PCB.TTSJF)
			System.out.println("RR5 ");
		
		System.out.println("The best Average WT : ");
		if(PCB.WTFCFS<= PCB.WTRR3 && PCB.WTFCFS<= PCB.WTRR5&& PCB.WTFCFS<= PCB.WTSJF)
			System.out.println("FCFS ");
		if(PCB.WTSJF<= PCB.WTRR3 && PCB.WTSJF<= PCB.WTRR5&& PCB.WTFCFS>= PCB.WTSJF)
			System.out.println("SJF ");
		if(PCB.WTFCFS>= PCB.WTRR3 && PCB.WTRR3<= PCB.WTRR5&& PCB.WTRR3<= PCB.WTSJF)
			System.out.println("RR3 ");
		if(PCB.WTRR5<= PCB.WTRR3 && PCB.WTRR5<= PCB.WTFCFS&& PCB.WTRR5<= PCB.WTSJF)
			System.out.println("RR5 ");
	}
	
	

}

class PCB {
	int id;
	int burstTime;
	int memory;
	int arrivalTime = 0;
	String state = null;
	public static double TTFCFS;
	public static double WTFCFS;
	public static double TTSJF;
	public static double WTSJF;
	public static double TTRR3;
	public static double WTRR3;
	public static double WTRR5;
	public static double TTRR5;

	public PCB(int id, int burstTime, int memory) {
		this.id = id;
		this.burstTime = burstTime;
		this.memory = memory;
		 state = "new";
	}
}
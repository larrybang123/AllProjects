package app_kvECS;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import shared.metadata.MetaData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

//import ecs.IECSNode;

public class ECSClient {
    private static Logger logger = Logger.getRootLogger();

    private static final String PROMPT = "ECSClient> ";
    private boolean exiting;
    private ECS ecs;

    // For testing purposes
    public MetaData metadata = null;
    public Collection<IECSNode> nodes = null;
    public boolean await_status = false;

    public ECSClient(String config_file) {
        this.ecs = new ECS(config_file);
    }

    // For JUnit test cases
    public ECSClient(String config_file, boolean testing) throws Exception {
        this.ecs = new ECS(config_file);
        this.exiting = false;
        this.run();
    }

    private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
	}

    public void handleCommand(String cmdLine) throws Exception {
        String[] tokens = cmdLine.split("\\s+");
        assert tokens.length > 0;

        if (tokens[0].equals("start")) {
            try {
                if (!ecs.start())
                    System.out.println("Error in start");
            } catch (Exception e) {
                logger.error("Error in start");
                e.printStackTrace();
            }
        }
        else if (tokens[0].equals("stop")) {
            try {
                if (!ecs.stop())
                    System.out.println("Error in stop");
            } catch (Exception e) {
                logger.error("Error in stop");
                e.printStackTrace();
            }
        }
		else if (tokens[0].equals("print")) {
			try {
				ecs.printAll();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
        else if (tokens[0].equals("shutdown")) {
            try {
                if (!ecs.shutdown())
                    System.out.println("Error in shutdown");
            } catch (Exception e) {
                logger.error("Error in shutdown");
                e.printStackTrace();
            }
        }
        else if (tokens[0].equals("addNode")) {
            if(tokens.length == 3) {
                try {
                    if (ecs.addNode(tokens[1], Integer.parseInt(tokens[2])) == null) {
                        printError("Unable to add server node.");
                    }
                } catch(NumberFormatException nfe) {
					printError("No valid cacheSize. cacheSize must be a number!");
					logger.info("Unable to parse argument <cacheSize>", nfe);
				} catch (Exception e) {
                    printError(e.getMessage());
                }
            } else {
                printError("Invalid number of parameters!");
            }
        }
        else if (tokens[0].equals("addNodes")) {
            if(tokens.length == 4) {
                try {
                    int count = Integer.parseInt(tokens[1]);
                    int cacheSize = Integer.parseInt(tokens[3]);
                    if (ecs.addNodes(count, tokens[2], cacheSize) == null) {
                        printError("Unable to add server node.");
                    }
                } catch(NumberFormatException nfe) {
                    printError("No valid count or cacheSize. They must be numbers!");
                    logger.info("Unable to parse argument <count> or <cacheSize>", nfe);
                } catch (Exception e) {
                    printError(e.getMessage());
                }
            } else {
                printError("Invalid number of parameters!");
            }
        }
        else if (tokens[0].equals("init")) {
            if(tokens.length == 4) {
                try {
                    int count = Integer.parseInt(tokens[1]);
                    int cacheSize = Integer.parseInt(tokens[3]);
                    this.nodes = ecs.setupNodes(count, tokens[2], cacheSize);
                    if (this.nodes == null) {
                        printError("Unable to initialize ECS.");
                    }
                } catch(NumberFormatException nfe) {
					printError("No valid count or cacheSize. They must be numbers!");
					logger.info("Unable to parse argument <count> or <cacheSize>", nfe);
				} catch (Exception e) {
                    printError(e.getMessage());
                }
            } else {
                printError("Invalid number of parameters!");
            }
        }
        else if (tokens[0].equals("removeNode")) {
            if(tokens.length == 2) {
                int index = Integer.parseInt(tokens[1]);
                if (ecs.removeNode(index) == null) {
                    printError("Unable to remove node. Index out of bounds.");
                }
            } else {
                printError("Invalid number of parameters!");
            }
        }
        else if (tokens[0].equals("getMetaData")) {
            this.metadata = this.ecs.getMetaData();
            if (this.metadata == null) {
                printError("Unable to retrieve metadata.");
            }
        }
        else if (tokens[0].equals("awaitNodes")) {
            if(tokens.length == 3) {
                int count = Integer.parseInt(tokens[1]);
                int timeout = Integer.parseInt(tokens[2]);
                try {
                    this.await_status = this.ecs.awaitNodes(count, timeout); 
                } catch (Exception e) {
                    printError(e.getMessage());
                }
            } else {
                printError("Invalid number of parameters!");
            }
        }
        else if (tokens[0].equals("quit")) {
            this.exiting = true;
			//disconnect();
			System.out.println(PROMPT + "Application exit!");
        } else {
            System.out.println("Invalid command.");
        }
    }

    public void run() throws Exception {
        BufferedReader stdin;
        while (!exiting) {
            stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);

            try {
                String cmdLine = stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                exiting = true;
				printError("CLI does not respond - Application terminated ");
			}
		}
	}

    public static void main(String[] args) {
        try {
            new LogSetup("logs/ecsclient.log", Level.OFF);
            if (args.length != 1) {
                System.out.println("Error! Invalid number of arguments!");
                System.out.println("Usage: java -jar ECS.jar <ecs_config_file>");
            } else {
                ECSClient app = new ECSClient(args[0]);
                app.run();
            }
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

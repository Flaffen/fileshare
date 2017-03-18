package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Client {
	public static void main(String... args) throws IOException {
		DatagramSocket sock = null;
		InetAddress host = args.length > 0 ? InetAddress.getByName(args[0]) : InetAddress.getByName("flaffen.ddns.net");
		int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
		BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
		DatagramPacket dp = null;
		
		try {
			sock = new DatagramSocket();
			
			logn("Finding port...");
			int PORT_RANGE_START = 50000;
			int PORT_RANGE_FINISH = 50100;
			sock.setSoTimeout(10);
			for (int i = PORT_RANGE_START; i <= PORT_RANGE_FINISH; i++) {
				log("Checking " + i + "\r");
				dp = new DatagramPacket("SEEK".getBytes(), "SEEK".getBytes().length, host, i);
				DatagramPacket rec = new DatagramPacket(new byte[65536], new byte[65536].length);
				
				try {
					sock.send(dp);
					sock.receive(rec);
					
					String response = new String(rec.getData()).trim();
					if (response.startsWith("CONFIRMED")) {
						port = rec.getPort();
						break;
					}
				} catch (SocketTimeoutException e) {
					continue;
				}
			}
			logn("");
			sock.setSoTimeout(0);
			logn("Port: " + port);
			
			logn("Sending 'HI' to a server...");
			dp = new DatagramPacket("HI".getBytes(), "HI".getBytes().length, host, port);
			sock.send(dp);
			
			logn("Waiting for headers...");
			byte[] buffer = new byte[65536];
			
			dp = new DatagramPacket(buffer, buffer.length);
			sock.receive(dp);
			int numberOfPackets = Integer.parseInt(new String(dp.getData(), 0, buffer.length).trim());
			
			dp = new DatagramPacket(buffer, buffer.length);
			sock.receive(dp);
			String fileName = new String(dp.getData(), 0, buffer.length).trim();
			
			buffer = new byte[65536];
			dp = new DatagramPacket(buffer, buffer.length);
			sock.receive(dp);
			int fileLength = Integer.parseInt(new String(dp.getData(), 0, buffer.length).trim());
			
			logn("Headers received.");
			logn("File name: " + fileName + ", number of packets to send: " + numberOfPackets);
			
			logn("Downloading the file...");
			ArrayList<Byte> totalBytes = new ArrayList<>();
			HashMap<Integer, String> packetVault = new HashMap<>();
			
			int packets = 0;
			int percentCounter = 0;
			for (int i = 1; i <= numberOfPackets; i++) {
				if (!packetVault.keySet().contains(i)) { 
					packets++;
					String message = "GET " + i;
					
					dp = new DatagramPacket(message.getBytes(), message.getBytes().length, host, port);
					sock.send(dp);
					
					buffer = new byte[65536];
					dp = new DatagramPacket(buffer, buffer.length);
					sock.setSoTimeout(2000);
					try {
						sock.receive(dp);
					} catch (Exception e) {
						dp = new DatagramPacket(message.getBytes(), message.getBytes().length, host, port);
						sock.send(dp);
						buffer = new byte[65536];
						dp = new DatagramPacket(buffer, buffer.length);
						sock.receive(dp);
					}
					
					String packet = new String(dp.getData(), 0, buffer.length).trim();
					int id = Integer.parseInt(packet.split(" ")[0]);
					String body = packet.substring(packet.indexOf(" ") + 1);
					
					packetVault.put(id, body);
//					logn(fileLength + " " + Math.round(packet.length()));
//					if (fileLength % (packet.length()) == 0) {
//						percentCounter++;
//						log("Downloading " + percentCounter + "%\r");
//					}
				}
			}
			System.out.println();
			logn(packets + " packets were downloaded.");
			
			sock.send(new DatagramPacket("STOP".getBytes(), "STOP".getBytes().length, host, port));
			
			for (int i = 1; i <= packetVault.size(); i++) {
				String body = packetVault.get(i);
				String[] stringBytes = body.split(", ");
				for (String stringByte : stringBytes) {
					totalBytes.add(Byte.parseByte(stringByte));
				}
			}
			
			System.out.println("Total size: " + totalBytes.size() + " bytes.");
			byte[] allBytes = new byte[totalBytes.size()];
			for (int i = 0; i < allBytes.length; i++) {
				allBytes[i] = totalBytes.get(i);
			}
			
			String folderPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator;
			System.out.print("Choose a directory to save a file (Enter for " + folderPath + "): ");
			String reqForFolder = cin.readLine();
			folderPath = reqForFolder.isEmpty() ? folderPath : reqForFolder;
			while (!new File(folderPath).isDirectory()) {
				System.out.println("No such folder!");
				folderPath = cin.readLine();
			}
			folderPath += File.separator;
			
			FileConverter.putToFile(Paths.get(folderPath + fileName), allBytes);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			sock.close();
		}
	}
	
	private static void log(Object o) {
		System.out.print(o);
	}
	
	private static void logn(Object o) {
		System.out.println(o);
	}
}

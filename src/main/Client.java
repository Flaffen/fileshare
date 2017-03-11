package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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
			// sock.setSoTimeout(10000);
			
			logn("Sending 'HI' to a server...");
			dp = new DatagramPacket("HI".getBytes(), "HI".getBytes().length, host, port);
			sock.send(dp);
			
			logn("Waiting for headers...");
			byte[] buffer = new byte[2048];
			
			dp = new DatagramPacket(buffer, buffer.length);
			sock.receive(dp);
			int numberOfPackets = Integer.parseInt(new String(dp.getData(), 0, buffer.length).trim());
			
			dp = new DatagramPacket(buffer, buffer.length);
			sock.receive(dp);
			String fileName = new String(dp.getData(), 0, buffer.length).trim();
			
			logn("Headers received.");
			logn("File name: " + fileName + ", number of packets to send: " + numberOfPackets);
			
			logn("Downloading the file...");
			int totalPackets = 0;
			int nextId = 1;
			ArrayList<Byte> totalBytes = new ArrayList<>();
			ArrayList<Integer> allIds = new ArrayList<>();
			HashMap<Integer, String> packetVault = new HashMap<>();
			while (true) {
				buffer = new byte[65536];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				try {
					sock.receive(reply);
				} catch (SocketTimeoutException e) {
					System.out.println("Socket timeout. Expected id: " + nextId);
					sock.send(new DatagramPacket("STOP".getBytes(), "STOP".getBytes().length, host, port));
					break;
				}
				
				byte[] data = reply.getData();
				String packet = new String(data, 0, buffer.length).trim();
				
				if (packet.startsWith("STOP")) break;
				
				int id = Integer.parseInt(packet.split(" ")[0]);
				allIds.add(id);
				
				if (id == nextId) {
					nextId++;
				} else {
					allIds.add(nextId + 1);
				}
				
				String body = packet.substring(packet.indexOf(" ") + 1);
				
				packetVault.put(id, body);
				totalPackets++;
			}
			
			logn("Downloading missing packets...");
			int missingPackets = 0;
			for (int i = 1; i <= numberOfPackets; i++) {
				if (!packetVault.keySet().contains(i)) { 
					missingPackets++;
					String message = "GET " + i;
					
					dp = new DatagramPacket(message.getBytes(), message.getBytes().length, host, port);
					sock.send(dp);
					
					buffer = new byte[65536];
					dp = new DatagramPacket(buffer, buffer.length);
					sock.receive(dp);
					
					String packet = new String(dp.getData(), 0, buffer.length).trim();
					int id = Integer.parseInt(packet.split(" ")[0]);
					String body = packet.substring(packet.indexOf(" ") + 1);
					
					packetVault.put(id, body);
				}
			}
			logn(missingPackets + " missing packets were downloaded.");
			
			for (int i = 1; i <= packetVault.size(); i++) {
				String body = packetVault.get(i);
				String[] stringBytes = body.split(", ");
				for (String stringByte : stringBytes) {
					totalBytes.add(Byte.parseByte(stringByte));
				}
			}
			
			sock.send(new DatagramPacket("STOP".getBytes(), "STOP".getBytes().length, host, port));
			
			System.out.println("Total size: " + totalBytes.size() + " bytes.");
			byte[] allBytes = new byte[totalBytes.size()];
			for (int i = 0; i < allBytes.length; i++) {
				allBytes[i] = totalBytes.get(i);
			}
			
			String folderPath = System.getProperty("user.home") + "\\Desktop\\";
			System.out.print("Choose a directory to save a file (Enter for " + folderPath + "): ");
			String reqForFolder = cin.readLine();
			folderPath = reqForFolder.isEmpty() ? folderPath : reqForFolder;
			if (!new File(folderPath).isDirectory()) {
				System.out.println("No such folder!");
				folderPath = cin.readLine();
			}
			
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

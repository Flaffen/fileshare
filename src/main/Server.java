package main;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Server {
	public static void main(String... args) throws IOException, InterruptedException {
		Random rand = new Random();
		int port = args.length > 0 ? Integer.parseInt(args[0]) : rand.nextInt(100) + 50000;
		DatagramSocket server = new DatagramSocket(port);
		Scanner scanner = null;
		String filePath = null;
		
		try {
			scanner = new Scanner(System.in);
			System.out.print("Choose a file to be sent to clients: ");
			filePath = scanner.nextLine();
			while (!new File(filePath).exists()) {
				System.out.println("No such file! Please, choose another: ");
				filePath = scanner.nextLine();
			}
		} finally {
			if (scanner != null) scanner.close();
		}
		
		Path file = Paths.get(filePath);
		byte[] fileBytes = FileConverter.convertToByteArray(file);
		String fileSize = fileBytes.length + "";
		System.out.println("File size: " + fileSize);
		
		String fileName = file.getFileName().toString();
		System.out.println("Filename: " + fileName);
		
		ArrayList<String> packets = new ArrayList<>();
		ArrayList<Byte> tempArray = new ArrayList<>();
		int id = 1;
		for (int i = 0; i < fileBytes.length; i++) {
			tempArray.add(fileBytes[i]);
			if (tempArray.size() == 256) {
				String packet = "";
				byte[] bts = new byte[tempArray.size()];
				for (int j = 0; j < bts.length; j++) {
					bts[j] = tempArray.get(j);
				}
				String temp = Arrays.toString(bts);
				packet = id + " " + String.join(" ", temp.substring(1, temp.length() - 1).split(" "));
				packets.add(packet);
				tempArray.clear();
				id++;
			} else if (i == fileBytes.length - 1) {
				String packet = "";
				byte[] bts = new byte[tempArray.size()];
				for (int j = 0; j < bts.length; j++) {
					bts[j] = tempArray.get(j);
				}
				String temp = Arrays.toString(bts);
				packet = id + " " + String.join(" ", temp.substring(1, temp.length() - 1).split(" "));
				packets.add(packet);
			}
		}
		
		byte[] buffer = new byte[65536];
		DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
		
		System.out.println("Running server on port " + port);
		System.out.println();
		
		while (true) {
			server.receive(incoming);
			byte[] data = incoming.getData();
			String s = new String(data, 0, incoming.getLength());
			InetAddress packetAddress = incoming.getAddress();
			int packetPort = incoming.getPort();
			
			if (s.startsWith("SEEK")) {
				server.send(new DatagramPacket("CONFIRMED".getBytes(), "CONFIRMED".getBytes().length, packetAddress, packetPort));
				continue;
			}
			
			System.out.println(packetAddress.getHostAddress() + " : " + packetPort + " - " + s);
			
			DatagramPacket dPacket = new DatagramPacket((packets.size() + "").getBytes(), (packets.size() + "").getBytes().length, packetAddress, packetPort);
			server.send(dPacket);
			
			dPacket = new DatagramPacket(fileName.getBytes(), fileName.getBytes().length, packetAddress, packetPort);
			server.send(dPacket);
			
			dPacket = new DatagramPacket(fileSize.getBytes(), fileSize.getBytes().length, packetAddress, packetPort);
			server.send(dPacket);
			System.out.println(fileSize);
			
//			for (String packet : packets) {
//				DatagramPacket dp = new DatagramPacket(packet.getBytes(), packet.getBytes().length, packetAddress, packetPort);
//				server.send(dp);
//			}
//			server.send(new DatagramPacket("STOP".getBytes(), "STOP".getBytes().length, packetAddress, packetPort));
			
			System.out.println("Sending the file... ");
			int percentCounter = 0;
			while (true) {
				byte[] bufferMiss = new byte[2048];
				DatagramPacket reqMissing = new DatagramPacket(bufferMiss, bufferMiss.length);
				
				server.receive(reqMissing);
				
				byte[] reqData = reqMissing.getData();
				String packet = new String(reqData, 0, bufferMiss.length).trim();
				
				if (packet.startsWith("STOP")) break;
				
				int missingPacket = Integer.parseInt(packet.split(" ")[1]);
				
				DatagramPacket response = new DatagramPacket(packets.get(missingPacket-1).getBytes(), packets.get(missingPacket-1).getBytes().length, reqMissing.getAddress(), reqMissing.getPort());
				server.send(response);
				/**
				if (missingPacket % (packets.size() / 100) == 0) {
					percentCounter++;
					System.out.print("Sending " + percentCounter + "%\r");
				}
				*/
			}
			System.out.println();
			
			System.out.println("All packets have been sent.");
			System.out.println();
		}
	}
}

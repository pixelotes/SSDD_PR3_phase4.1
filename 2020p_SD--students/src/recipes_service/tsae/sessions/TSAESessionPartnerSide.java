/*
* Copyright (c) Joan-Manuel Marques 2013. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This file is part of the practical assignment of Distributed Systems course.
*
* This code is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This code is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this code.  If not, see <http://www.gnu.org/licenses/>.
*/

package recipes_service.tsae.sessions;


import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import communication.ObjectInputStream_DS;
import communication.ObjectOutputStream_DS;
import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;
import recipes_service.ServerData;
import recipes_service.communication.Message;
import recipes_service.communication.MessageAErequest;
import recipes_service.communication.MessageEndTSAE;
import recipes_service.communication.MessageOperation;
import recipes_service.communication.MsgType;
import recipes_service.data.AddOperation;
import recipes_service.data.Operation;
import recipes_service.data.OperationType;
import recipes_service.data.RemoveOperation;
import recipes_service.tsae.data_structures.TimestampMatrix;
import recipes_service.tsae.data_structures.TimestampVector;

//import lsim.library.api.LSimLogger;

/**
 * @author Joan-Manuel Marques
 * December 2012
 *
 */
public class TSAESessionPartnerSide extends Thread{
	
	private Socket socket = null;
	private ServerData serverData = null;
	
	public TSAESessionPartnerSide(Socket socket, ServerData serverData) {
		super("TSAEPartnerSideThread");
		this.socket = socket;
		this.serverData = serverData;
	}

	public void run() {

		Message msg = null;

		int current_session_number = -1;
		try {
			ObjectOutputStream_DS out = new ObjectOutputStream_DS(socket.getOutputStream());
			ObjectInputStream_DS in = new ObjectInputStream_DS(socket.getInputStream());

			// receive request from originator and update local state
			// receive originator's summary and ack
			msg = (Message) in.readObject();
			current_session_number = msg.getSessionNumber();
			//LSimLogger.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] TSAE session");
			//LSimLogger.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] received message: "+ msg);
			if (msg.type() == MsgType.AE_REQUEST){
				// Declaramos una nueva variable de tipo MessageAErequest
				MessageAErequest messageAER = (MessageAErequest) msg;
				
				
				// Variables necesarias para enviar al originador
				TimestampVector localSummary;
				TimestampMatrix localAck;
				
				// M�todo necesario para el uso de threads y actualizar el sum del server
				synchronized (serverData) {
                    localSummary = serverData.getSummary().clone();
                    serverData.getAck().update(serverData.getId(), localSummary);
                    localAck = serverData.getAck().clone();
                }
				
				// Obtenemos los logs
				List<Operation> newLogs = serverData.getLog().listNewer(messageAER.getSummary());
				// Recorremos todos los logs
				for (Operation op : newLogs) {
					out.writeObject(new MessageOperation(op));
				}
				
				
				msg = new MessageAErequest(localSummary, localAck);
				msg.setSessionNumber(current_session_number);
	 	        out.writeObject(msg);
				//LSimLogger.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] sent message: "+ msg);

				List<MessageOperation> listOperations = new ArrayList<MessageOperation>();

				
	            // receive operations
				msg = (Message) in.readObject();
				//LSimLogger.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] received message: "+ msg);
				while (msg.type() == MsgType.OPERATION){
					
					listOperations.add((MessageOperation) msg);
					msg = (Message) in.readObject();
					//LSimLogger.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] received message: "+ msg);
				}
				
				// receive message to inform about the ending of the TSAE session
				if (msg.type() == MsgType.END_TSAE){
					// send and "end of TSAE session" message
					msg = new MessageEndTSAE();
					msg.setSessionNumber(current_session_number);
		            out.writeObject(msg);					
					//LSimLogger.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] sent message: "+ msg);
					
					// Update server's Summary and Ackowledgement vectors
					synchronized (serverData) {
					    // Recorremos la lista de operaciones
	                    for (MessageOperation eachOperation : listOperations) {
	                       	// Revisamos si la operacion a realizar es de tipo a�adir o eliminar
	                        if (eachOperation.getOperation().getType() == OperationType.ADD) {
	                           	// A�adimos la operacion
	                            serverData.addOperation((AddOperation) eachOperation.getOperation());
	                        } else {
	                           	// Removemos la operacion
	                            serverData.removeOperation((RemoveOperation) eachOperation.getOperation());
	                        }
	                    }

	                    serverData.getSummary().updateMax(messageAER.getSummary());
	                    serverData.getAck().updateMax(messageAER.getAck());
	                    serverData.getLog().purgeLog(serverData.getAck());
	                }
				}
				
			}
			socket.close();		
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			//LSimLogger.log(Level.FATAL, "[TSAESessionPartnerSide] [session: "+current_session_number+"]" + e.getMessage());
			e.printStackTrace();
            System.exit(1);
		}catch (IOException e) {
	    }
		
		//LSimLogger.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] End TSAE session");
	}
}

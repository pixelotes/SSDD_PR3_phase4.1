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

package recipes_service.tsae.data_structures;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;

/**
 * @author Joan-Manuel Marques, Daniel Lázaro Iglesias
 * December 2012
 *
 */
public class TimestampMatrix implements Serializable{
	
	private static final long serialVersionUID = 3331148113387926667L;
	ConcurrentHashMap<String, TimestampVector> timestampMatrix = new ConcurrentHashMap<String, TimestampVector>();
	
	public TimestampMatrix(List<String> participants){
		// create and empty TimestampMatrix
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			timestampMatrix.put(it.next(), new TimestampVector(participants));
		}
	}
	
	public TimestampMatrix() {
		super();
	}
	
	/**
	 * @param node
	 * @return the timestamp vector of node in this timestamp matrix
	 */
	TimestampVector getTimestampVector(String node){
		//Como recibimos por parametro la key del hashmap, simplemente accedemos y retornamos su value
		return timestampMatrix.get(node);
	}
	
	/**
	 * Merges two timestamp matrix taking the elementwise maximum
	 * @param tsMatrix
	 */
	public synchronized void updateMax(TimestampMatrix tsMatrix){
		// Accedemos al hash del timestramp matrix pasado por parametro e iteramos sus keys
		for (Map.Entry<String, TimestampVector> keys : tsMatrix.timestampMatrix.entrySet()) {
			// Obtenemos la clave y declaramos los dos timestamp para actualizar el maximo
			String key = keys.getKey();
			// Guardamos los dos timestamp en dos variables para compararlos posteriormente
			TimestampVector timestampVector1 = keys.getValue();
			TimestampVector timestampVector2 = this.timestampMatrix.get(key);
			
			// Revisamos que no sea null antes de actualizar
			if (timestampVector2 != null) {
				// Finalmente actualizamos
				timestampVector2.updateMax(timestampVector1);
			}
		}
	}
	
	/**
	 * substitutes current timestamp vector of node for tsVector
	 * @param node
	 * @param tsVector
	 */
	public synchronized void update(String node, TimestampVector tsVector) {
		// Evaluamos el caso en el que tengamos que actualizar o reemplazar
		if (this.timestampMatrix.get(node) != null) {
			this.timestampMatrix.replace(node, tsVector);
		} else {
			this.timestampMatrix.put(node, tsVector);
		}
	}
	
	/**
	 * 
	 * @return a timestamp vector containing, for each node, 
	 * the timestamp known by all participants
	 */
	public synchronized TimestampVector minTimestampVector(){
		TimestampVector timestampVector = null;
		// Iteramos los values del hashmap
		for (TimestampVector eachTimestampVector : this.timestampMatrix.values()) {
			// Si el timestamp es null lo clonamos y en caso contrario llamamos al metodo merge
			if (timestampVector == null)
				timestampVector = eachTimestampVector.clone();
			else
				timestampVector.mergeMin(eachTimestampVector);
		}
		// Finalmente retornamos el timestampVector
		return timestampVector;
	}
	
	/**
	 * clone
	 */
	public synchronized TimestampMatrix clone(){
		// Declaramos e inicializamos el timestamap que sera clonado
		TimestampMatrix timestampCloner = new TimestampMatrix();
		// Iteramos las keys del hashmap
		for (Map.Entry<String, TimestampVector> data : timestampMatrix.entrySet()) {
			timestampCloner.timestampMatrix.put(data.getKey(), data.getValue().clone());
		}
		// Finalmente devolvemos el timestamp clonado
		return timestampCloner;
	}
	
	/**
	 * equals
	 */
	@Override
	public boolean equals(Object obj) {
		// Comparamos el obj que exista y devolvemos true si existe
		if (this == obj)
			return true;
		// Comparamos el obj que exista y devolvemos false si no existe
		if (obj == null)
			return false;
		// Comparamos que el obj sea de la clase que queremos, sino devolvemos false
		if (getClass() != obj.getClass())
			return false;
		// Si los dos Log son iguales devolvemos el valor
		TimestampMatrix newLog = (TimestampMatrix) obj;
		return newLog.timestampMatrix.equals(timestampMatrix);
	}
	
	
	/**
	 * toString
	 */
	@Override
	public synchronized String toString() {
		String all="";
		if(timestampMatrix==null){
			return all;
		}
		for(Enumeration<String> en=timestampMatrix.keys(); en.hasMoreElements();){
			String name=en.nextElement();
			if(timestampMatrix.get(name)!=null)
				all+=name+":   "+timestampMatrix.get(name)+"\n";
		}
		return all;
	}
}

package org.igov.model.enums;

import org.igov.activiti.common.Dimension;

public enum Form {
		
	STRING_HW{
		/**
		 * Возвращает размер текстового
		 */
		@Override
		public Dimension getDimension() {			
			return new Dimension("30","300");
		}
	},
	STRING_W{
		/**
		 * Возвращает размер текстового
		 */
		@Override
		public Dimension getDimension() {
			Dimension dimension = new Dimension();
			dimension.setWidth("300");
			return dimension;
		}
	},
	STRING_H{
		/**
		 * Возвращает размер текстового
		 */
		@Override
		public Dimension getDimension() {
			Dimension dimension = new Dimension();
			dimension.setHeight("30");
			return dimension;
		}
	};
	
	
	
	/**
	 * Возвращает специфические размеры формы
	 * @return
	 */
	public abstract Dimension getDimension();	
	

}
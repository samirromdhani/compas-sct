module org.lfenergy.compas.sct.header {

	exports org.lfenergy.compas.sct.header;
	exports org.lfenergy.compas.sct.header.impl;

//	requires org.lfenergy.compas.scl2007b4.model;
//	requires org.lfenergy.compas.scl2007b4.model.SCL;
//	requires org.lfenergy.compas.core;

	provides org.lfenergy.compas.sct.header.IHeaderService
			with org.lfenergy.compas.sct.header.impl.HeaderService;
}
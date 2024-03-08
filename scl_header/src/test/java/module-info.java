
module org.lfenergy.compas.sct.test {

	requires org.lfenergy.compas.sct.header;
	requires org.junit.jupiter.api;
	requires org.assertj.core;
	requires org.junit.jupiter.engine;
	requires org.junit.platform.launcher;
	requires org.junit.platform.engine;
	opens org.lfenergy.compas.sct.test to org.junit.platform.commons;
}

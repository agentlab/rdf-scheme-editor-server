//package ru.agentlab.rdf4j.jaxrs;
//
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//import org.ops4j.pax.exam.junit.PaxExam;
//import org.ops4j.pax.exam.junit.PaxExamParameterized;
//import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
//import org.ops4j.pax.exam.spi.reactors.PerClass;
//import org.osgi.framework.BundleContext;
//
//import javax.inject.Inject;
//import java.util.Arrays;
//import java.util.Collection;
//
//@RunWith(PaxExamParameterized.class)
//@ExamReactorStrategy(PerClass.class)
//public class tted  extends Rdf4jJaxrsTestSupport{
//
//
//    String mystr;
//    String juststr;
//
//
//    @Inject
//    public void sdstted(String mystr, String juststr){
//        this.mystr = mystr;
//        this.juststr = juststr;
//    }
//
////    @Before
////    public void inti(){
////        ;
////    }
//
//    @Parameterized.Parameters
//    public static Collection data(){
//        return Arrays.asList(new Object[] []{
//                {"memory", "ssds"}, {"native","sdsd"}, {"native-rdfs", "dsdsds"}
//        });
//    }
//
//
//    @Test
//    public void myTest(){
//        System.out.println(mystr+ " " + juststr);
//    }
//
//}

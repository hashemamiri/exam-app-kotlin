package ir.exam.app.core.image
import ir.exam.app.domain.model.QuestionImage
import org.junit.Assert.assertEquals
import org.junit.Test
class A4ImagePositionerTest {
 @Test fun `move keeps image inside A4 boundary`() { val start=QuestionImage(xMm=100f,yMm=30f,widthMm=70f); val end=A4ImagePositioner.move(start,5000f,0f,10f); assertEquals(140f,end.xMm) }
}

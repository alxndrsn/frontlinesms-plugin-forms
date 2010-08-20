/**
 * 
 */
package net.frontlinesms.plugins.forms.data.repository.hibernate;

import java.util.List;

import net.frontlinesms.junit.HibernateTestCase;
import net.frontlinesms.plugins.forms.data.domain.Form;
import net.frontlinesms.plugins.forms.data.domain.FormResponse;
import net.frontlinesms.plugins.forms.data.domain.ResponseValue;
import net.frontlinesms.plugins.forms.data.repository.FormDao;
import net.frontlinesms.plugins.forms.data.repository.FormResponseDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateFormResponseDao}
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class HibernateFormResponseDaoTest1 extends HibernateTestCase {
	
//> STATIC CONSTANTS
	
//> INSTANCE PROPERTIES
	@Autowired
	private FormDao formDao;
	@Autowired
	private FormResponseDao formResponseDao;

//> TEST METHODS
	public void testGetFormResponses() {
		testGetFormResponses(2, 31, 28);
		testGetFormResponses(5, 47, 38);
		
		for(int i=2; i<6; i+=3) {
			Form form = this.formDao.getFromId(i);
			System.out.println(i + ": " + formResponseDao.getFormResponseCount(form));
			
			List<FormResponse> rz = this.formResponseDao.getFormResponses(form, 0, 100);
			for(FormResponse r : rz) {
				for(ResponseValue p : r.getResults()) {
					System.out.print("\t" + p.toString());
				}
				System.out.println();
			}
			System.out.println();
		}
	}
	
	private void testGetFormResponses(int formId, int expectedFieldCount, int expectedResponseCount) {
		Form form = this.formDao.getFromId(formId);
		// There may be fields without values returned here
		assertTrue(form.getFields().size() >= expectedFieldCount);
		
		List<FormResponse> rz = this.formResponseDao.getFormResponses(form, 0, expectedResponseCount * 2);
		assertEquals(expectedResponseCount, rz.size());
		for(FormResponse r : rz) {
			assertEquals(expectedFieldCount, r.getResults().size());
		}
	}

//> ACCESSORS
	/** @param formDao The DAO to use for the test. */
	@Required
	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	/** @param formDao The DAO to use for the test. */
	@Required
	public void setFormResponseDao(FormResponseDao formResponseDao) {
		this.formResponseDao = formResponseDao;
	}
}

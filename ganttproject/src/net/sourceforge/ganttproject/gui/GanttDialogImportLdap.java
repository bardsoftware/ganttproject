package net.sourceforge.ganttproject.gui;

import java.awt.Component;

import java.awt.event.ActionEvent;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import javax.swing.JTabbedPane;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.StringOption;


public class GanttDialogImportLdap {

	private JTabbedPane tabbedPane;

	private boolean change = false;

	private final GPOptionGroup myGroup;
	private final UIFacade myUIFacade;
	private final HumanResourceManager myHumanResourceManager;
	private static final GanttLanguage language = GanttLanguage.getInstance();

	private final StringOption myBaseDNField = new DefaultStringOption("Base DN");
	private final StringOption myServerField = new DefaultStringOption("server");
	private final StringOption myFilterField = new DefaultStringOption("Filter");
	private final StringOption myUsernameField = new DefaultStringOption("userName");
	private final StringOption myPasswordField = new DefaultStringOption("password");

	private SearchControls ctls;

	public GanttDialogImportLdap(UIFacade uiFacade,HumanResourceManager hrm) {
		myUIFacade = uiFacade;
		myPasswordField.setScreened(true);
		myFilterField.setValue("(&(objectClass=user)(objectcategory=person))");
		myHumanResourceManager = hrm;
		myGroup = new GPOptionGroup("", new GPOption[] { myServerField,myBaseDNField,myFilterField,myUsernameField,myPasswordField });
		myGroup.setTitled(false);
	}

	private Component getComponent( ) {
		OptionsPageBuilder builder = new OptionsPageBuilder();
	    OptionsPageBuilder.I18N i18n = new OptionsPageBuilder.I18N() {
	      @Override
	      public String getOptionLabel(GPOptionGroup group, GPOption<?> option) {
	        return getValue(option.getID());
	      }
	    };
	    builder.setI18N(i18n);
	    final JComponent mainPage = builder.buildPlanePage(new GPOptionGroup[] { myGroup });
	    mainPage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	    tabbedPane = new JTabbedPane();

	    tabbedPane.addTab(language.getText("general"), new ImageIcon(getClass().getResource("/icons/properties_16.gif")),
	            mainPage);

	    return tabbedPane;

	}

	public void setVisible(boolean isVisible) {
		if (isVisible) {
		      //loadFields();
		      Component contentPane = getComponent();
		      OkAction okAction = new OkAction() {
		        @Override
		        public void actionPerformed(ActionEvent e) {
		          myGroup.commit();
		          okButtonActionPerformed();
		        }
		      };
		      CancelAction cancelAction = new CancelAction() {
		        @Override
		        public void actionPerformed(ActionEvent e) {
		          myGroup.rollback();
		          change = false;
		        }
		      };
		      myUIFacade.createDialog(contentPane, new Action[] { okAction, cancelAction }, language.getCorrectedLabel("human")).show();
		    }
	}

	private void okButtonActionPerformed() {
		ctls = new SearchControls();
		String[] returnedAttrs = { "sn","givenName","samAccountName","mail","userPrincipalName","displayName" };
		ctls.setReturningAttributes(returnedAttrs);
		ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		try {
			String providerURL = myServerField.getValue();

			Hashtable<String,Object> env = new Hashtable<>();
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			env.put(Context.SECURITY_PRINCIPAL, myUsernameField.getValue());
			env.put(Context.SECURITY_CREDENTIALS, myPasswordField.getValue());
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, providerURL);
			env.put("java.naming.ldap.attributes.binary", "objectSID");
			InitialDirContext context = new InitialDirContext(env);

			NamingEnumeration<SearchResult> sre = context.search(myBaseDNField.getValue(), myFilterField.getValue() , ctls);

			while(sre.hasMoreElements()) {
				SearchResult sr = sre.next();

				if(sr.getAttributes().get("displayName") != null) {
					HumanResource hr = myHumanResourceManager.newHumanResource();
					hr.setName(sr.getAttributes().get("displayName").get().toString());
					myHumanResourceManager.add(hr);
				}
			}
			sre.close();


		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}

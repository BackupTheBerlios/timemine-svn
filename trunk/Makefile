# ----------------------------------------------------------------------------
#
# $Source: $
# $Revision: 1.6 $
# $Author: torsten $
# Contents: Makefile for Timemine
# Systems: all
#			   
# ----------------------------------------------------------------------------

#---------------------------------- settings ---------------------------------

PLATFORM           = Linux
JAVA_DATA_MODEL    := $(shell if test ! -f classes/DataModel.class; then install -d classes; javac -d classes src/DataModel.java; fi; java -cp classes DataModel)

ENABLE_DEBUG       = no

#------------------------------------ paths ----------------------------------

SWT_DIR            = 
LAUNCH4J_DIR       = 

#--------------------------------- tool chain --------------------------------

JAVA                = java                          
JAVA_FLAGS          = -Xmx256m                      

JAVAC               = javac                         
JAVAC_FLAGS         = -source 1.5                   
# -Xlint

JAR                 = jar                           

SWT_JAR_LINUX       = jars/linux/swt-3.7.2.jar
SWT_JAR_LINUX_64    = jars/linux/swt-3.7.2_64.jar
SWT_JAR_WINDOWS     = jars/windows/swt-3.7.2.jar
SWT_JAR_WINDOWS_64  = jars/windows/swt-3.7.2_64.jar
SWT_JAR_SOLARIS     = jars/solaris/swt-3.7.2.jar
SWT_JAR_MACOSX      = jars/macosx/swt-3.7.2.jar     
SWT_JAR_MACOSX_64   = jars/macosx/swt-3.7.2_64.jar

ifneq ($(SWT_DIR),)
SWT_JAR             = $(SWT_DIR)/swt.jar
else
ifeq ($(PLATFORM),Linux)
ifeq ($(JAVA_DATA_MODEL),64)
SWT_JAR             = $(SWT_JAR_LINUX_64)
else
SWT_JAR             = $(SWT_JAR_LINUX)
endif
endif
ifeq ($(PLATFORM),Windows)
ifeq ($(JAVA_DATA_MODEL),64)
SWT_JAR             = $(SWT_JAR_WINDOWS_64)
else
SWT_JAR             = $(SWT_JAR_WINDOWS)
endif
endif
ifeq ($(PLATFORM),Solaris)
SWT_JAR             = $(SWT_JAR_SOLARIS)
endif
ifeq ($(PLATFORM),MacOSX)
ifeq ($(JAVA_DATA_MODEL),64)
SWT_JAR             = $(SWT_JAR_MACOSX_64)
else
SWT_JAR             = $(SWT_JAR_MACOSX)
endif
endif
endif

APACHE_COMMONS_JARS = jars/commons-codec-1.7.jar

CLASSPATH           = classes:$(SWT_JAR):$(APACHE_COMMONS_JARS)

SHELL = bash

#---------------------------------- commands----------------------------------

CAT        = cat
CD         = cd
CHMOD      = chmod
CP         = cp
DIFF       = diff
ECHO       = echo
ECHO_NO_LF = echo -n
GREP       = grep
INSTALL    = /usr/bin/install -c
MD5SUM     = md5sum
MKDIR      = mkdir
PERL       = /usr/bin/perl
RMDIR      = rmdir
RMF        = rm -f
RMRF       = rm -rf
SED        = sed
TAR        = tar
ZIP        = zip
UNZIP      = unzip

UNOCONV    = unoconv
TXT2MAN    = txt2man

#------------------------ specific variables/settings ------------------------

VERSION_MAJOR := $(shell $(CAT) version|$(GREP) MAJOR|$(SED) 's/MAJOR=//g')
VERSION_MINOR := $(shell $(CAT) version|$(GREP) MINOR|$(SED) 's/MINOR=//g')

DISTRIBUTION_NAME = timemine-$(VERSION_MAJOR).$(VERSION_MINOR)

#---------------------------------- rules ------------------------------------

classes/%.class: src/%.java
	$(INSTALL) -d classes
ifneq ($(JAVAC),)
	$(JAVAC) $(JAVAC_FLAGS) -d classes -classpath $(CLASSPATH) src/*.java
else
	@$(ECHO) "ERROR: no 'javac' command - cannot compile Java sources"
endif

#--------------------------------- objects -----------------------------------

SOURCES        = src/Timemine.java \
                 src/Settings.java \
                 src/Redmine.java \
                 \
                 src/TableLayout.java \
                 src/TableLayoutData.java \
                 src/Pane.java \
                 src/ProgressBar.java \
                 src/Widgets.java \
                 src/Dialogs.java \
                 src/StringParser.java \
                 src/StringUtils.java \
                 src/Option.java \
                 src/Options.java \
                 src/OptionEnumeration.java \
                 src/OptionSpecial.java \
                 src/Dprintf.java \
                 src/SoftHashMap.java \
                 \
                 src/DataModel.java \
                 \
                 src/Config.java \

OTHER_SOURCES  = timemine.in \
                 timemine.bat.in \
                 jar.txt \

OBJECTS        = $(foreach z,$(SOURCES),$(basename $(subst src/,classes/,$z)).class) \
                 classes/Config.class \

JARS           = timemine-linux.jar \
                 timemine-linux_64.jar \
                 timemine-windows.jar \
                 timemine-windows_64.jar \
                 timemine-solaris.jar \
                 timemine-macosx.jar \
                 timemine-macosx_64.jar \

OTHER_JARS     = $(APACHE_COMMONS_JARS) \

TARGETS        = timemine \
                 $(if $(LAUNCH4J),timemine.exe,timemine.bat) \

DOC            = 

#------------------------------ dependencies ---------------------------------

.PHONY: all clean distclean depend
all: \
  $(TARGETS) \
  $(DOC)\

clean: \
  clean_jars
	$(RMF) src/Config.java
	$(RMF) $(OBJECTS)
	$(RMF) classes/*.class
	$(RMF) $(TARGETS)
	$(RMRF) tmp

distclean: \
  clean

# ----------------------------------------------------------------------------
# config
src/Config.java: src/Config.java.in
	$(CAT) src/Config.java.in | \
        ( \
          source version; \
          while IFS="" read line; do \
            line=$${line//@VERSION_MAJOR@/$$MAJOR}; \
            line=$${line//@VERSION_MINOR@/$$MINOR}; \
            line=$${line//@VERSION_REVISION@/$(shell svnversion .)}; \
            $(ECHO) "$$line"; \
          done; \
        ) > $@       

# ----------------------------------------------------------------------------
# JARs

.PHONY: jars clean_jars
jars: $(JARS)

clean_jars:
	$(RMF) $(JARS)

.PHONY: compile
compile: $(OBJECTS)

# create JARs
timemine-linux.jar: src/Config.java jar.txt $(OBJECTS) images/*.png $(SWT_JAR_LINUX)
ifneq ($(JAR),)
	$(RMRF) tmp/jar; $(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_LINUX) 1>/dev/null; $(RMRF) META-INF)
	# add Apache commons JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(APACHE_COMMONS_JARS) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

timemine-linux_64.jar: src/Config.java jar.txt $(OBJECTS) images/*.png $(SWT_JAR_LINUX_64)
ifneq ($(JAR),)
	$(RMRF) tmp/jar; $(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_LINUX_64) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add Apache commons JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(APACHE_COMMONS_JARS) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

timemine-windows.jar: src/Config.java jar.txt $(OBJECTS) images/*.png $(SWT_JAR_WINDOWS)
ifneq ($(JAR),)
	$(RMRF) tmp/jar; $(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_WINDOWS) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add Apache commons JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(APACHE_COMMONS_JARS) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

timemine-windows_64.jar: src/Config.java jar.txt $(OBJECTS) images/*.png $(SWT_JAR_WINDOWS_64)
ifneq ($(JAR),)
	$(RMRF) tmp/jar; $(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_WINDOWS_64) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add Apache commons JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(APACHE_COMMONS_JARS) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

timemine-solaris.jar: src/Config.java jar.txt $(OBJECTS) images/*.png $(SWT_JAR_SOLARIS)
ifneq ($(JAR),)
	$(RMRF) tmp/jar; $(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_SOLARIS) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add Apache commons JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(APACHE_COMMONS_JARS) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

timemine-macosx.jar: src/Config.java jar.txt $(OBJECTS) images/*.png $(SWT_JAR_MACOSX)
ifneq ($(JAR),)
	$(RMRF) tmp/jar; $(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_MACOSX) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add Apache commons JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(APACHE_COMMONS_JARS) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

timemine-macosx_64.jar: src/Config.java jar.txt $(OBJECTS) images/*.png $(SWT_JAR_MACOSX_64)
ifneq ($(JAR),)
	$(RMRF) tmp/jar; $(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_MACOSX_64) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add Apache commons JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(APACHE_COMMONS_JARS) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

# create Unix start script
timemine: timemine.in timemine-linux.jar timemine-linux_64.jar
	$(CAT) timemine.in | \
        ( \
          while IFS="" read line; do \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA\@|$(JAVA)|g'`; \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA_FLAGS\@|$(JAVA_FLAGS)|g'`; \
            $(ECHO) "$$line"; \
          done; \
        ) > $@
	$(CHMOD) 775 $@

ifneq ($(LAUNCH4J),)
# create Windows executable
timemine.exe: timemine-windows.jar timemine-windows_64.jar
	$(LAUNCH4J) timemine.xml
else
# create Windows start script
timemine.bat: timemine.bat.in timemine-windows.jar timemine-windows_64.jar
	$(CAT) timemine.bat.in | \
        ( \
          while IFS="" read line; do \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA\@|$(JAVA)|g'`; \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA_FLAGS\@|$(JAVA_FLAGS)|g'`; \
            $(ECHO) "$$line"; \
          done; \
        ) > $@
	$(CHMOD) 775 $@
endif

.PHONY: linux
linux: timemine timemine-linux.jar timemine-linux_64.jar

.PHONY: windows
windows: timemine.bat timemine-windows.jar timemine-windows_64.jar

.PHONY: solaris
solaris: timemine timemine-solaris.jar timemine-solaris_64.jar

.PHONY: macosx
macosx: timemine timemine-macosx.jar timemine-macosx_64.jar

# ----------------------------------------------------------------------------
# run
run: $(OBJECTS)
ifneq ($(JAVA),)
	$(JAVA) $(JAVA_FLAGS) -classpath $(CLASSPATH) Timemine $(OPTIONS) --name admin --password admin
else
	@$(ECHO) "ERROR: no 'java' command - cannot execute Timemine"
endif
rundebug: $(OBJECTS)
ifneq ($(JAVA),)
	$(JAVA) $(JAVA_FLAGS) -enableassertions -classpath $(CLASSPATH) Timemine --debug $(OPTIONS) --name admin --password admin
else
	@$(ECHO) "ERROR: no 'java' command - cannot execute Timemine"
endif
runxdebug: $(OBJECTS)
ifneq ($(JAVA),)
	$(JAVA) $(JAVA_FLAGS) -enableassertions -classpath $(CLASSPATH) Timemine --debug $(OPTIONS) --name admin --password admin
else
	@$(ECHO) "ERROR: no 'java' command - cannot execute Timemine"
endif

# run with JAR file
ifneq ($(JAVA),)
runjar: timemine
	./timemine
else
	@$(ECHO) "ERROR: no 'java' command - cannot execute Timemine"
endif

.PHONY: install
#install: $(JARS) timemine
install: timemine-linux.jar timemine
	$(INSTALL) -d $(HOME)/bin
	$(INSTALL) -m 664 \
                   timemine-linux.jar \
                   timemine-linux_64.jar \
                   $(HOME)/bin
	$(INSTALL) -m 775 \
                   timemine \
                   $(HOME)/bin

# ----------------------------------------------------------------------------
# create documentation
.PHONY: doc clean_doc man clean_man showman

doc: \
  doc/timemine.7 \
  doc/timemine.pdf

clean_doc:
ifneq ($(TXT2MAN),)
	$(RMF) doc/timemine.7
endif
ifneq ($(UNOCONV),)
	$(RMF) doc/timemine.pdf
endif

# create man-page

man: \
  doc/timemine.7

clean_man:
	$(RMF) doc/timemine.7

doc/timemine.7: \
  doc/timemine.txt
ifneq ($(TXT2MAN),)
	$(CAT) doc/timemine.txt | $(TXT2MAN) -t timemine -r $(VERSION_MAJOR).$(VERSION_MINOR) -s 7 -v "Linux User's Manual" > $@
else
	@$(ECHO) "ERROR: no 'txt2man' tool available! Cannot create man page."
endif

showman:
ifneq ($(TXT2MAN),)
	$(CAT) doc/timemine.txt | $(TXT2MAN) -t timemine -r $(VERSION_MAJOR).$(VERSION_MINOR) -s 7 -v "Linux User's Manual" -T
else
	@$(ECHO) "ERROR: no 'txt2man' tool available! Cannot create man page."
endif

# create manual

doc/timemine.pdf: \
  doc/timemine.odt
ifneq ($(UNOCONV),)
	$(UNOCONV) -f pdf doc/timemine.odt
else
	@$(ECHO) "ERROR: no 'unoconv' tool available! Cannot create PDF manual."
endif

# ----------------------------------------------------------------------------
# distribution

.PHONY: dist clean_dist $(DISTRIBUTION_NAME).tar.bz2

dist: \
  $(DISTRIBUTION_NAME).tar.bz2 \
  $(DISTRIBUTION_NAME)-linux.tar.bz2 \
  $(DISTRIBUTION_NAME)-solaris.tar.bz2 \
  $(DISTRIBUTION_NAME)-macosx.tar.bz2 \
  $(DISTRIBUTION_NAME)-windows.zip
ifneq ($(MD5SUM),)
	$(MD5SUM) $^
endif

clean_dist:
	$(RMF) $(DISTRIBUTION_NAME).tar.bz2
	$(RMF) $(DISTRIBUTION_NAME)-linux.tar.bz2
	$(RMF) $(DISTRIBUTION_NAME)-solaris.tar.bz2
	$(RMF) $(DISTRIBUTION_NAME)-macosx.tar.bz2
	$(RMF) $(DISTRIBUTION_NAME)-windows.zip

# create distribution
$(DISTRIBUTION_NAME).tar.bz2: \
  $(TARGETS) \
  \
  Makefile \
  version \
  ToDo \
  ChangeLog \
  \
  $(OTHER_SOURCES) \
  $(OTHER_JARS) \
  \
  $(SCRIPTS) \
  \
  doc/README \
  doc/COPYING \
  doc/images/*.png \
  $(DOC)
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)
	$(INSTALL) -m 775 \
                   $(OTHER_SOURCES) \
                   tmp/$(DISTRIBUTION_NAME)
	$(INSTALL) -m 664 \
                   Makefile \
                   version \
                   ToDo \
                   ChangeLog \
                   tmp/$(DISTRIBUTION_NAME)
	#
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/src
	$(INSTALL) -m 664 \
                   src/Config.java.in \
                   $(sort $(SOURCES)) \
                   tmp/$(DISTRIBUTION_NAME)/src
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/classes
	$(INSTALL) -m 664 \
                   $(sort $(OTHER_SOURCES)) \
                   tmp/$(DISTRIBUTION_NAME)
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/images
	$(INSTALL) -m 664 \
                   images/*.png \
                   tmp/$(DISTRIBUTION_NAME)/images
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/jars
	$(INSTALL) -m 664 \
                   $(OTHER_JARS) \
                   tmp/$(DISTRIBUTION_NAME)/jars
	#
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/jars/linux
	$(INSTALL) -m 664 \
                   jars/linux/*.jar \
                   tmp/$(DISTRIBUTION_NAME)/jars/linux
	#
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/jars/solaris
	$(INSTALL) -m 664 \
                   jars/solaris/*.jar \
                   tmp/$(DISTRIBUTION_NAME)/jars/solaris
	#
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/jars/macosx
	$(INSTALL) -m 664 \
                   jars/macosx/*.jar \
                   tmp/$(DISTRIBUTION_NAME)/jars/macosx
	#
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/jars/windows
	$(INSTALL) -m 664 \
                   jars/windows/*.jar \
                   tmp/$(DISTRIBUTION_NAME)/jars/windows
	#
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/doc
	$(INSTALL) -m 664 \
                   doc/README \
                   doc/COPYING \
                   tmp/$(DISTRIBUTION_NAME)/doc
	$(INSTALL) -d tmp/$(DISTRIBUTION_NAME)/doc/images
	$(INSTALL) -m 664 \
                   doc/images/*.png \
                   tmp/$(DISTRIBUTION_NAME)/doc/images
	#
	($(CD) tmp; $(TAR) cjf ../$@ $(DISTRIBUTION_NAME))
	$(RMRF) tmp/$(DISTRIBUTION_NAME)
	#
ifneq ($(MD5SUM),)
	$(MD5SUM) $@
endif

$(DISTRIBUTION_NAME)-linux.tar.bz2: \
  timemine-linux.jar \
  timemine-linux_64.jar \
  timemine
	$(TAR) cjf $@ \
          timemine-linux.jar timemine-linux_64.jar \
          timemine

$(DISTRIBUTION_NAME)-solaris.tar.bz2: \
  timemine-solaris.jar \
  timemine
	$(TAR) cjf $@ \
          timemine-solaris.jar \
          timemine

$(DISTRIBUTION_NAME)-macosx.tar.bz2: \
  timemine-macosx.jar \
  timemine-macosx_64.jar \
  timemine
	$(TAR) cjf $@ \
          timemine-macosx.jar timemine-macosx_64.jar \
          timemine

$(DISTRIBUTION_NAME)-windows.zip: \
  timemine-windows.jar \
  timemine-windows_64.jar \
  timemine.bat
	$(ZIP) $@ \
          timemine-windows.jar timemine-windows_64.jar \
          timemine.bat

.PHONY: upload
upload:
	( \
          $(ECHO) $(DISTRIBUTION_NAME).tar.bz2; \
          $(ECHO) $(DISTRIBUTION_NAME)-linux.tar.bz2; \
          $(ECHO) $(DISTRIBUTION_NAME)-solaris.tar.bz2; \
          $(ECHO) $(DISTRIBUTION_NAME)-macosx.tar.bz2; \
          $(ECHO) $(DISTRIBUTION_NAME)-windows.zip; \
        ) | \
        xargs ftp-upload -v --host ftp.berlios.de --user anonymous --password torsten.rupp@gmx.net --dir /incoming --full-path --binary
# end of file

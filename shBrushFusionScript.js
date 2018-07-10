SyntaxHighlighter.brushes.Custom = function()
{
    var keywords = 'INTEGER DOUBLE LONG BOOLEAN DATE DATETIME TEXT STRING ISTRING VARISTRING VARSTRING TIME RICHTEXT ' +
        'ABSTRACT ACTION ACTIVE ACTIVATE ADDFORM NEW AFTER ' +
        'AGGR ALL AND APPEND APPLY AS ASON ASSIGN ASYNCUPDATE ATTACH '+
        'ATTR AUTO AUTOREFRESH AUTOSET BACKGROUND BCC BEFORE BODY BOTTOM BREAK BY CANCEL CANONICALNAME ' +
        'CASE CATCH CC CENTER CHANGE CHANGECLASS CHANGED CHANGEWYS CHARSET CHECK ' +
        'CHECKED CLASS CLIENT CLOSE COLOR COLUMNS COMPLEX CONCAT CONFIRM CONNECTION CONSTRAINT ' +
        'CONTAINERH CONTAINERV CONTEXTFILTER CSV CUSTOM CUSTOMFILE CUSTOMLINK CYCLES DATA DBF DEFAULT DEFAULTCOMPARE DELAY DELETE ' +
        'DESC DESIGN DIALOG DO DOC DOCKED DOCKEDMODAL DOCX DRAWROOT ' +
        'DROP DROPCHANGED DROPSET ECHO EDIT EDITABLE EDITFORM EDITKEY ' +
        'ELSE EMAIL END EQUAL EVAL EVENTID EVENTS EXCELFILE EXCELLINK ' +
        'EXCEPTLAST EXCLUSIVE EXEC EXPORT EXTEND EXTERNAL FALSE FIELDS FILE FILTER FILTERGROUP ' +
        'FILTERS FINALLY FIRST FIXED FIXEDCHARWIDTH FOCUS FOLDER FOOTER FOR FORCE FOREGROUND ' +
        'FORM FORMS FORMULA FROM FULL FULLSCREEN GOAFTER GRID GROUP GROUPCHANGE HALIGN HEADER ' +
        'HIDE HIDESCROLLBARS HIDETITLE HINTNOUPDATE HINTTABLE HORIZONTAL ' +
        'HTML HTTP IF IMAGE IMAGEFILE IMAGELINK IMPORT IMPOSSIBLE IN INCREMENT INDEX ' +
        'INDEXED INIT INITFILTER INLINE INPUT IS JAVA JOIN JSON KEYPRESS LAST LEFT LENGTH LIKE LIMIT ' +
        'LIST LOADFILE LOCAL LOGGABLE LSF MANAGESESSION MAX MAXCHARWIDTH MDB ' +
        'MEMO MESSAGE META MIN MINCHARWIDTH MODAL MODULE MOVE MS MULTI NAGGR NAME NAMESPACE ' +
        'NAVIGATOR NESTED NEW NEWEXECUTOR NEWSESSION NEWSQL NEWTHREAD NO NOCANCEL NOESCAPE NOHEADER NOHINT NONULL NOT NOWAIT NULL NUMERIC OBJECT ' +
        'OBJECTS OK ON OPEN OPTIMISTICASYNC OR ORDER OVERRIDE PAGESIZE ' +
        'PANEL PARENT PARTITION PASSWORD PDF PDFFILE RAWFILE PDFLINK RAWLINK PERIOD MATERIALIZED PG POSITION ' +
        'PREFCHARWIDTH PREV PRINT PRIORITY PROPERTIES PROPERTY ' +
        'PROPORTION QUERYOK QUERYCLOSE QUICKFILTER READ READONLY READONLYIF RECURSION REFLECTION REGEXP REMOVE ' +
        'REPORTFILES REQUEST REQUIRE RESOLVE RETURN RGB RIGHT ROOT ' +
        'ROUND RTF SCHEDULE SCROLL SEEK SELECTOR SESSION SET SETCHANGED SHORTCUT SHOW SHOWDROP ' +
        'SHOWIF SINGLE SHEET SPLITH SPLITV SQL START STEP STRETCH STRICT STRUCT SUBJECT ' +
        'SUM TAB TABBED TABLE TEXTHALIGN TEXTVALIGN THEN THREADS TIME TO TODRAW ' +
        'TOOLBAR TOP TREE TRUE TRY UNGROUP UPDATE VALIGN VALUE ' +
        'VERTICAL VIEW WHEN WHERE WHILE WINDOW WORDFILE WORDLINK WRITE XLS XLSX XML XOR YES';

    this.regexList = [
        { regex: SyntaxHighlighter.regexLib.singleLineCComments, css: 'color1' },
        { regex: /#{2,3}/gi, css: 'color2' },
        { regex: /@[a-zA-Z]\w*\b/gi, css: 'color2' },
        { regex: SyntaxHighlighter.regexLib.singleQuotedString, css: 'value' },
        { regex: /\b\d+l?\b/gi, css: 'value' },
        { regex: /\b\d+\.\d*d?\b/gi, css: 'value' },
        { regex: /\b\d{4}_\d\d_\d\d(_\d\d:\d\d)?\b/gi, css: 'value' },
        { regex: /\b\d\d:\d\d\b/gi, css: 'value' },
        { regex: /#[0-9a-fA-F]{6}/gi, css: 'value' },
        { regex: new RegExp(this.getKeywords(keywords), 'gm'), css: 'keyword' }
    ];
};

SyntaxHighlighter.brushes.Custom.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.Custom.aliases = ['custom', 'lsf', 'ls'];


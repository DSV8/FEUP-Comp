// Generated from comp2024/grammar/IPmm.g4 by ANTLR 4.5.3

    package pt.up.fe.comp2024;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class IPmmParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		IP=1, IP_num=2, WS=3;
	public static final int
		RULE_ips_set = 0, RULE_ip_adress = 1;
	public static final String[] ruleNames = {
		"ips_set", "ip_adress"
	};

	private static final String[] _LITERAL_NAMES = {
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "IP", "IP_num", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "IPmm.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public IPmmParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class Ips_setContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(IPmmParser.EOF, 0); }
		public List<Ip_adressContext> ip_adress() {
			return getRuleContexts(Ip_adressContext.class);
		}
		public Ip_adressContext ip_adress(int i) {
			return getRuleContext(Ip_adressContext.class,i);
		}
		public Ips_setContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ips_set; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IPmmListener ) ((IPmmListener)listener).enterIps_set(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IPmmListener ) ((IPmmListener)listener).exitIps_set(this);
		}
	}

	public final Ips_setContext ips_set() throws RecognitionException {
		Ips_setContext _localctx = new Ips_setContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_ips_set);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(5); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(4);
				ip_adress();
				}
				}
				setState(7); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==IP );
			setState(9);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Ip_adressContext extends ParserRuleContext {
		public Token adress;
		public TerminalNode IP() { return getToken(IPmmParser.IP, 0); }
		public Ip_adressContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ip_adress; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IPmmListener ) ((IPmmListener)listener).enterIp_adress(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IPmmListener ) ((IPmmListener)listener).exitIp_adress(this);
		}
	}

	public final Ip_adressContext ip_adress() throws RecognitionException {
		Ip_adressContext _localctx = new Ip_adressContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_ip_adress);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(11);
			((Ip_adressContext)_localctx).adress = match(IP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\5\20\4\2\t\2\4\3"+
		"\t\3\3\2\6\2\b\n\2\r\2\16\2\t\3\2\3\2\3\3\3\3\3\3\2\2\4\2\4\2\2\16\2\7"+
		"\3\2\2\2\4\r\3\2\2\2\6\b\5\4\3\2\7\6\3\2\2\2\b\t\3\2\2\2\t\7\3\2\2\2\t"+
		"\n\3\2\2\2\n\13\3\2\2\2\13\f\7\2\2\3\f\3\3\2\2\2\r\16\7\3\2\2\16\5\3\2"+
		"\2\2\3\t";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should create a deposit transaction"
    request {
        method 'POST'
        url '/cash/deposit'
        headers {
            contentType(applicationJson())
        }
        body(
                login: "test_user",
                amount: 100.00
        )
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
                id: 1,
                accountLogin: "test_user",
                transactionType: "DEPOSIT",
                amount: 100.00,
                status: "COMPLETED"
        )
    }
}

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should create a withdrawal transaction"
    request {
        method 'POST'
        url '/cash/withdraw'
        headers {
            contentType(applicationJson())
        }
        body(
                login: "test_user",
                amount: 50.00
        )
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
                id: 2,
                accountLogin: "test_user",
                transactionType: "WITHDRAW",
                amount: 50.00,
                status: "COMPLETED"
        )
    }
}
